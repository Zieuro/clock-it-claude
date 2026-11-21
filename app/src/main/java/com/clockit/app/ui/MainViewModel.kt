package com.clockit.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.clockit.app.data.Alarm
import com.clockit.app.data.ScheduleRepository

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ScheduleRepository.get(app)
    val state = repo.state

    fun setArmed(armed: Boolean) = repo.setArmed(armed)
    fun save(alarm: Alarm) = repo.upsert(if (alarm.id == 0) alarm.copy(id = repo.nextId()) else alarm)
    fun delete(id: Int) = repo.delete(id)
    fun setEnabled(id: Int, enabled: Boolean) = repo.setEnabled(id, enabled)
    fun clear() = repo.clear()
    fun applyTemplate(times: List<Int>, title: String, replace: Boolean) = repo.applyTemplate(times, title, replace)
    fun resync() = repo.resync()
}
