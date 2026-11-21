package com.clockit.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.clockit.app.MainActivity
import com.clockit.app.data.ScheduleState
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Only the single next fire time is registered with the system. When it fires, the receiver
 * schedules the following one. This keeps us well below AlarmManager's per-app alarm limit
 * no matter how dense an interval schedule is.
 */
object AlarmScheduler {
    const val ACTION_FIRE = "com.clockit.app.FIRE"
    const val ACTION_SNOOZE_FIRE = "com.clockit.app.SNOOZE_FIRE"
    const val EXTRA_TRIGGER = "trigger"
    const val EXTRA_TITLE = "title"

    private const val REQ_NEXT = 100
    private const val REQ_SNOOZE = 101

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun sync(context: Context, state: ScheduleState, ref: LocalDateTime = LocalDateTime.now()) {
        val am = context.getSystemService(AlarmManager::class.java)
        val next = if (state.armed) state.nextFire(ref) else null
        if (next == null) {
            am.cancel(firePendingIntent(context, 0L))
            if (!state.armed) am.cancel(snoozePendingIntent(context, ""))
            return
        }
        val millis = next.first.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAlarm(context, millis, firePendingIntent(context, millis))
    }

    fun snooze(context: Context, title: String, minutes: Int) {
        val millis = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(context, millis, snoozePendingIntent(context, title))
    }

    private fun setAlarm(context: Context, millis: Long, op: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, showIntent(context)), op)
        } catch (_: SecurityException) {
            // Exact alarm permission revoked: fall back to the best inexact option.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, op)
        }
    }

    private fun firePendingIntent(context: Context, trigger: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQ_NEXT,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_TRIGGER, trigger),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun snoozePendingIntent(context: Context, title: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQ_SNOOZE,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_SNOOZE_FIRE).putExtra(EXTRA_TITLE, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun showIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
