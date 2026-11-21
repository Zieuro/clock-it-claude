package com.clockit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.clockit.app.ui.theme.Blue
import com.clockit.app.ui.theme.BlueBright
import com.clockit.app.ui.theme.BlueDeep
import com.clockit.app.ui.theme.Outline
import com.clockit.app.ui.theme.Surface1
import com.clockit.app.ui.theme.Surface2
import com.clockit.app.ui.theme.TextMid

/** Large pill switch that arms or disarms the entire schedule. */
@Composable
fun ArmToggle(armed: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptics = LocalHapticFeedback.current
    val trackBorder by animateColorAsState(if (armed) Blue.copy(alpha = 0.6f) else Outline, label = "border")
    val label by animateColorAsState(if (armed) Color.White else TextMid, label = "label")

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(CircleShape)
            .background(
                if (armed) Brush.horizontalGradient(listOf(BlueDeep.copy(alpha = 0.55f), Blue.copy(alpha = 0.35f)))
                else Brush.horizontalGradient(listOf(Surface1, Surface1))
            )
            .border(1.dp, trackBorder, CircleShape)
            .toggleable(
                value = armed,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onChange(it)
            }
            .padding(8.dp),
    ) {
        val thumb = 60.dp
        val travel = maxWidth - thumb
        val x by animateDpAsState(
            if (armed) travel else 0.dp,
            spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "thumb",
        )

        Text(
            if (armed) "ARMED" else "DISARMED",
            Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleMedium,
            color = label,
            letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing * 4,
        )

        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .size(thumb)
                .shadow(if (armed) 14.dp else 2.dp, CircleShape, ambientColor = Blue, spotColor = Blue)
                .clip(CircleShape)
                .background(
                    if (armed) Brush.linearGradient(listOf(BlueBright, Blue))
                    else Brush.linearGradient(listOf(Surface2, Surface2))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (armed) Icons.Rounded.AlarmOn else Icons.Rounded.AlarmOff,
                contentDescription = null,
                tint = if (armed) Color.White else TextMid,
            )
        }
    }
}
