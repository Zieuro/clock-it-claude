package com.clockit.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF0A0D13)
val Surface1 = Color(0xFF121721)
val Surface2 = Color(0xFF1A202C)
val Outline = Color(0xFF262E3D)
val Blue = Color(0xFF3D8BFF)
val BlueBright = Color(0xFF5CC8FF)
val BlueDeep = Color(0xFF1E5BD8)
val TextHigh = Color(0xFFEAF0FA)
val TextMid = Color(0xFF9AA6BA)
val TextLow = Color(0xFF5F6B80)
val Danger = Color(0xFFFF5C6C)

private val Colors = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF15305C),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = BlueBright,
    onSecondary = Color(0xFF00243A),
    secondaryContainer = Color(0xFF1A2A44),
    onSecondaryContainer = Color(0xFFCFE0FF),
    background = Bg,
    onBackground = TextHigh,
    surface = Bg,
    onSurface = TextHigh,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMid,
    surfaceContainerLowest = Bg,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Color(0xFF222938),
    outline = Outline,
    outlineVariant = Outline,
    error = Danger,
)

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Light),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    )
}

val TimeDigits = TextStyle(fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp)

@Composable
fun ClockItTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography) {
        CompositionLocalProvider(LocalContentColor provides TextHigh, content = content)
    }
}
