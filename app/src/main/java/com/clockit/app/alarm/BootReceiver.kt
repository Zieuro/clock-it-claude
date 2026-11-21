package com.clockit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clockit.app.data.ScheduleRepository

/** Re-registers the next alarm after reboot, app update, clock or permission changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        runCatching { ScheduleRepository.get(context).resync() }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
