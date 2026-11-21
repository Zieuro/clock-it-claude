package com.clockit.app.data

import android.content.Context
import android.content.SharedPreferences
import com.clockit.app.alarm.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for the alarm schedule. Persists to SharedPreferences and
 * re-syncs the system alarm whenever anything changes.
 */
class ScheduleRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("schedule", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    val current: ScheduleState get() = _state.value

    fun setArmed(armed: Boolean) = update { it.copy(armed = armed) }

    fun upsert(alarm: Alarm) = update { s ->
        val exists = s.alarms.any { it.id == alarm.id }
        val list = if (exists) s.alarms.map { if (it.id == alarm.id) alarm else it } else s.alarms + alarm
        s.copy(alarms = list.sortedBy { it.minuteOfDay })
    }

    fun delete(id: Int) = update { s -> s.copy(alarms = s.alarms.filterNot { it.id == id }) }

    fun clear() = update { it.copy(alarms = emptyList()) }

    fun setEnabled(id: Int, enabled: Boolean) = update { s ->
        s.copy(alarms = s.alarms.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    /** Adds alarms from the interval template, optionally replacing the current schedule. */
    fun applyTemplate(times: List<Int>, title: String, replace: Boolean) = update { s ->
        val base = if (replace) emptyList() else s.alarms
        val taken = base.map { it.minuteOfDay }.toMutableSet()
        val added = times.filter { taken.add(it) }.map { m ->
            Alarm(id = nextId(), hour = m / 60, minute = m % 60, title = title.trim())
        }
        s.copy(alarms = (base + added).sortedBy { it.minuteOfDay })
    }

    fun nextId(): Int {
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply()
        return id
    }

    /** Re-schedules the system alarm from the current state. */
    fun resync() = AlarmScheduler.sync(context, _state.value)

    private fun update(transform: (ScheduleState) -> ScheduleState) {
        val newState = transform(_state.value)
        _state.value = newState
        save(newState)
        AlarmScheduler.sync(context, newState)
    }

    private fun load(): ScheduleState {
        val armed = prefs.getBoolean(KEY_ARMED, false)
        val raw = prefs.getString(KEY_ALARMS, null) ?: return ScheduleState(armed = armed)
        val alarms = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Alarm(
                    id = o.getInt("id"),
                    hour = o.getInt("h"),
                    minute = o.getInt("m"),
                    title = o.optString("t", ""),
                    enabled = o.optBoolean("e", true),
                )
            }
        }.getOrDefault(emptyList())
        return ScheduleState(alarms.sortedBy { it.minuteOfDay }, armed)
    }

    private fun save(s: ScheduleState) {
        val arr = JSONArray()
        s.alarms.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("h", it.hour).put("m", it.minute)
                    .put("t", it.title).put("e", it.enabled)
            )
        }
        prefs.edit().putBoolean(KEY_ARMED, s.armed).putString(KEY_ALARMS, arr.toString()).apply()
    }

    companion object {
        private const val KEY_ARMED = "armed"
        private const val KEY_ALARMS = "alarms"
        private const val KEY_NEXT_ID = "next_id"

        @Volatile private var instance: ScheduleRepository? = null

        fun get(context: Context): ScheduleRepository =
            instance ?: synchronized(this) {
                instance ?: ScheduleRepository(context.applicationContext).also { instance = it }
            }
    }
}
