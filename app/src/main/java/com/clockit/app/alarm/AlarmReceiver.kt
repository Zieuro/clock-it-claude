package com.clockit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockit.app.data.ScheduleRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = ScheduleRepository.get(context)
        val state = repo.current
        when (intent.action) {
            AlarmScheduler.ACTION_FIRE -> {
                val triggerMillis = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER, System.currentTimeMillis())
                val trigger = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerMillis), ZoneId.systemDefault())
                if (state.armed) {
                    val minute = trigger.hour * 60 + trigger.minute
                    val due = state.activeAlarms.filter { it.minuteOfDay == minute }
                    if (due.isNotEmpty()) {
                        val title = due.map { it.title }.filter { it.isNotBlank() }.joinToString(" · ")
                        AlarmService.start(context, title, trigger.hour, trigger.minute)
                    }
                }
                // Queue the following alarm, strictly after the one that just fired.
                val now = LocalDateTime.now()
                AlarmScheduler.sync(context, state, if (trigger.isAfter(now)) trigger else now)
            }
            AlarmScheduler.ACTION_SNOOZE_FIRE -> {
                if (state.armed) {
                    val now = LocalDateTime.now()
                    AlarmService.start(context, intent.getStringExtra(AlarmScheduler.EXTRA_TITLE).orEmpty(), now.hour, now.minute)
                }
            }
        }
    }
}
