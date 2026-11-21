package com.clockit.app.ui

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.clockit.app.alarm.AlarmScheduler
import com.clockit.app.alarm.formatClock
import com.clockit.app.data.ScheduleState
import com.clockit.app.ui.components.ArmToggle
import com.clockit.app.ui.components.CountdownRing
import com.clockit.app.ui.theme.Blue
import com.clockit.app.ui.theme.BlueBright
import com.clockit.app.ui.theme.Outline
import com.clockit.app.ui.theme.Surface1
import com.clockit.app.ui.theme.TextHigh
import com.clockit.app.ui.theme.TextLow
import com.clockit.app.ui.theme.TextMid
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun HomeScreen(state: ScheduleState, onArm: (Boolean) -> Unit, onOpenSchedule: () -> Unit, onPermissionChange: () -> Unit) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    val next = state.nextFire(now)
    val prev = state.previousFire(now)

    val remaining = next?.let { Duration.between(now, it.first) }
    val progress = if (next != null && prev != null) {
        val total = Duration.between(prev, next.first).toMillis().coerceAtLeast(1)
        remaining!!.toMillis().toFloat() / total
    } else 0f

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Clock It", style = MaterialTheme.typography.headlineMedium)
                Text(
                    formatClock(LocalContext.current, now.hour, now.minute) + "  ·  " +
                        now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium, color = TextMid,
                )
            }
            StatusDot(state.armed && next != null)
        }

        PermissionBanners(onPermissionChange)

        Spacer(Modifier.height(20.dp))

        CountdownRing(
            progress = if (state.armed) progress else 1f,
            active = state.armed && next != null,
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
        ) {
            RingCenter(state, now, next?.first, next?.second?.map { it.title }, remaining)
        }

        Spacer(Modifier.height(28.dp))

        ArmToggle(
            armed = state.armed,
            onChange = onArm,
            enabled = state.alarms.isNotEmpty() || state.armed,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Text(
            when {
                state.alarms.isEmpty() -> "Add alarms to your schedule to arm it"
                state.armed -> "${state.activeAlarms.size} alarm${if (state.activeAlarms.size == 1) "" else "s"} armed · repeats daily"
                else -> "Tap to arm all ${state.activeAlarms.size} active alarms"
            },
            style = MaterialTheme.typography.bodySmall, color = TextLow,
            modifier = Modifier.padding(top = 10.dp),
        )

        Spacer(Modifier.height(28.dp))
        UpNext(state, now, onOpenSchedule)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RingCenter(
    state: ScheduleState,
    now: LocalDateTime,
    nextAt: LocalDateTime?,
    titles: List<String>?,
    remaining: Duration?,
) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(36.dp)) {
        if (nextAt == null || remaining == null) {
            Text("No alarms", style = MaterialTheme.typography.headlineSmall, color = TextMid)
            Text("Build a schedule to begin", style = MaterialTheme.typography.bodySmall, color = TextLow)
            return
        }
        Text(
            if (state.armed) "NEXT ALARM IN" else "DISARMED",
            style = MaterialTheme.typography.labelMedium,
            color = if (state.armed) BlueBright else TextLow,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            formatCountdown(remaining),
            fontSize = if (remaining.toHours() >= 10) 46.sp else 54.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
            color = if (state.armed) TextHigh else TextMid,
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        val day = if (nextAt.toLocalDate() == now.toLocalDate()) "" else "Tomorrow · "
        Text(
            day + formatClock(context, nextAt.hour, nextAt.minute),
            style = MaterialTheme.typography.titleMedium,
            color = if (state.armed) Blue else TextLow,
        )
        val title = titles.orEmpty().filter { it.isNotBlank() }.joinToString(" · ")
        if (title.isNotEmpty()) {
            Text(
                title, style = MaterialTheme.typography.bodyMedium, color = TextMid,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusDot(on: Boolean) {
    Row(
        Modifier
            .background(if (on) Blue.copy(alpha = 0.12f) else Surface1, CircleShape)
            .border(1.dp, if (on) Blue.copy(alpha = 0.4f) else Outline, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(if (on) BlueBright else TextLow, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(if (on) "Active" else "Idle", style = MaterialTheme.typography.labelMedium, color = if (on) BlueBright else TextMid)
    }
}

@Composable
private fun UpNext(state: ScheduleState, now: LocalDateTime, onOpenSchedule: () -> Unit) {
    val context = LocalContext.current
    val items = state.upcoming(now, 4)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Up next", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenSchedule) { Text("Edit schedule") }
        }
        if (items.isEmpty()) {
            Text("Nothing scheduled.", color = TextLow, style = MaterialTheme.typography.bodyMedium)
        }
        items.forEachIndexed { i, (at, alarm) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Surface1, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(if (i == 0 && state.armed) BlueBright else Outline, CircleShape))
                Spacer(Modifier.width(14.dp))
                Text(
                    formatClock(context, alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.armed) TextHigh else TextMid,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    alarm.title.ifBlank { "Alarm" }, style = MaterialTheme.typography.bodyMedium, color = TextMid,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(
                    if (at.toLocalDate() == LocalDate.now()) "in " + formatShort(Duration.between(now, at)) else "tmrw",
                    style = MaterialTheme.typography.labelMedium, color = TextLow,
                )
            }
        }
    }
}

@Composable
private fun PermissionBanners(onChange: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    LaunchedEffect(tick) { onChange() }

    val exactOk = remember(tick) { AlarmScheduler.canScheduleExact(context) }
    val fullScreenOk = remember(tick) {
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val notifOk = remember(tick) { context.getSystemService(NotificationManager::class.java).areNotificationsEnabled() }

    AnimatedVisibility(!exactOk) {
        Banner("Exact alarms are off, so alarms may be late.", "Allow") {
            if (Build.VERSION.SDK_INT >= 31) context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            )
        }
    }
    AnimatedVisibility(exactOk && !notifOk) {
        Banner("Notifications are blocked. Alarms can't ring.", "Enable") {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    }
    AnimatedVisibility(exactOk && notifOk && !fullScreenOk) {
        Banner("Allow full-screen alarms to show over the lock screen.", "Allow") {
            if (Build.VERSION.SDK_INT >= 34) context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
            )
        }
    }
}

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.WarningAmber, null, tint = BlueBright)
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action) }
    }
}

fun formatCountdown(d: Duration): String {
    val total = d.seconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun formatShort(d: Duration): String {
    val mins = (d.seconds + 59) / 60
    return if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
}
