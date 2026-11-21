package com.clockit.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.clockit.app.R
import com.clockit.app.ui.RingActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RingInfo(val title: String, val hour: Int, val minute: Int)

/** Foreground service that plays the alarm sound and shows the full-screen ringing UI. */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> stopRinging()
            ACTION_SNOOZE -> {
                val title = _ringing.value?.title.orEmpty()
                AlarmScheduler.snooze(this, title, SNOOZE_MINUTES)
                stopRinging()
            }
            else -> {
                val info = RingInfo(
                    intent?.getStringExtra(EXTRA_TITLE).orEmpty(),
                    intent?.getIntExtra(EXTRA_HOUR, 0) ?: 0,
                    intent?.getIntExtra(EXTRA_MINUTE, 0) ?: 0,
                )
                startRinging(info)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRinging(info: RingInfo) {
        _ringing.value = info
        ensureChannel(this)
        val notification = buildNotification(info)
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )

        if (wakeLock == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ClockIt:ring")
                .apply { acquire(AUTO_STOP_MS + 5_000) }
        }
        if (player == null) startSound()
        if (vibrator == null) startVibration()

        // Try to bring up the ringing screen directly; the full-screen intent covers the locked case.
        runCatching {
            startActivity(Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, AUTO_STOP_MS)
    }

    private fun startSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun startVibration() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        }
        if (!v.hasVibrator()) return
        val pattern = longArrayOf(0, 600, 400, 600, 400)
        val effect = VibrationEffect.createWaveform(pattern, 0)
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
        vibrator = v
    }

    private fun stopRinging() {
        handler.removeCallbacks(autoStop)
        player?.runCatching { stop(); release() }
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (_ringing.value != null) stopRinging()
        super.onDestroy()
    }

    private fun buildNotification(info: RingInfo): Notification {
        val fullScreen = PendingIntent.getActivity(
            this, 0,
            Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val time = formatClock(this, info.hour, info.minute)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(info.title.ifBlank { "Alarm" })
            .setContentText(time)
            .setColor(ContextCompat.getColor(this, R.color.accent))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Snooze $SNOOZE_MINUTES min", actionIntent(ACTION_SNOOZE, 1))
            .addAction(0, "Dismiss", actionIntent(ACTION_DISMISS, 2))
            .build()
    }

    private fun actionIntent(action: String, req: Int): PendingIntent =
        PendingIntent.getService(
            this, req, Intent(this, AlarmService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val SNOOZE_MINUTES = 5
        private const val AUTO_STOP_MS = 10 * 60_000L
        private const val CHANNEL_ID = "alarms"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_START = "start"
        private const val ACTION_DISMISS = "dismiss"
        private const val ACTION_SNOOZE = "snooze"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_HOUR = "hour"
        private const val EXTRA_MINUTE = "minute"

        private val _ringing = MutableStateFlow<RingInfo?>(null)
        val ringing: StateFlow<RingInfo?> = _ringing.asStateFlow()

        fun start(context: Context, title: String, hour: Int, minute: Int) {
            val i = Intent(context, AlarmService::class.java).setAction(ACTION_START)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_HOUR, hour).putExtra(EXTRA_MINUTE, minute)
            ContextCompat.startForegroundService(context, i)
        }

        fun dismiss(context: Context) =
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS))

        fun snooze(context: Context) =
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE))

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Ringing alarms"
                    setSound(null, null) // the service plays the alarm sound itself
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }
}

fun formatClock(context: Context, hour: Int, minute: Int): String {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    return if (is24) "%02d:%02d".format(hour, minute)
    else "%d:%02d %s".format(if (hour % 12 == 0) 12 else hour % 12, minute, if (hour < 12) "AM" else "PM")
}
