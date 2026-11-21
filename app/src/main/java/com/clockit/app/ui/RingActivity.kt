package com.clockit.app.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockit.app.alarm.AlarmService
import com.clockit.app.alarm.RingInfo
import com.clockit.app.alarm.formatClock
import com.clockit.app.ui.theme.Bg
import com.clockit.app.ui.theme.Blue
import com.clockit.app.ui.theme.BlueBright
import com.clockit.app.ui.theme.ClockItTheme
import com.clockit.app.ui.theme.TextHigh
import com.clockit.app.ui.theme.TextMid

/** Full-screen ringing UI shown over the lock screen. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ClockItTheme {
                val info by AlarmService.ringing.collectAsStateWithLifecycle()
                val seen = remember { booleanArrayOf(false) }
                LaunchedEffect(info) {
                    if (info != null) seen[0] = true
                    else if (seen[0]) finish()
                }
                RingScreen(
                    info = info,
                    onSnooze = { AlarmService.snooze(this); finish() },
                    onDismiss = { AlarmService.dismiss(this); finish() },
                )
            }
        }
    }
}

@Composable
private fun RingScreen(info: RingInfo?, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Blue.copy(alpha = 0.25f), Bg, Bg)))
            .safeDrawingPadding()
            .padding(32.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(160.dp).scale(scale).background(Blue.copy(alpha = 0.15f), CircleShape))
                Box(Modifier.size(112.dp).background(Brush.linearGradient(listOf(BlueBright, Blue)), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Alarm, null, tint = TextHigh, modifier = Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(40.dp))
            Text(
                info?.let { formatClock(context, it.hour, it.minute) } ?: "",
                style = MaterialTheme.typography.displayLarge, color = TextHigh,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                info?.title?.ifBlank { "Alarm" } ?: "Alarm",
                style = MaterialTheme.typography.headlineSmall, color = TextMid, textAlign = TextAlign.Center,
            )
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
            ) { Text("Dismiss", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
            ) { Text("Snooze ${AlarmService.SNOOZE_MINUTES} min", color = TextMid) }
        }
    }
}
