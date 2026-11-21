package com.clockit.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.clockit.app.data.Alarm
import com.clockit.app.data.ScheduleState
import com.clockit.app.ui.HomeScreen
import com.clockit.app.ui.IntervalTemplateSheet
import com.clockit.app.ui.theme.Bg
import com.clockit.app.ui.theme.ClockItTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-night-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun sample(armed: Boolean): ScheduleState {
        val t = LocalTime.now()
        val base = t.hour * 60 + t.minute
        val alarms = listOf(-40, 25, 55, 85, 115).mapIndexed { i, off ->
            val m = ((base + off) % 1440 + 1440) % 1440
            Alarm(i + 1, m / 60, m % 60, listOf("Wake up", "Stretch", "", "Leave for work", "Stand-up")[i])
        }
        return ScheduleState(alarms, armed)
    }

    private fun shoot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { ClockItTheme { Box(Modifier.fillMaxSize().background(Bg)) { content() } } }
        compose.mainClock.advanceTimeBy(1500)
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun homeArmed() = shoot("home_armed") { HomeScreen(sample(true), {}, {}, {}) }
    @Test fun schedule() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val repo = com.clockit.app.data.ScheduleRepository.get(app)
        repo.clear()
        repo.applyTemplate(listOf(390, 405, 420, 435), "Wake up", replace = true)
        repo.upsert(Alarm(repo.nextId(), 8, 30, "Leave for work"))
        val first = repo.current.alarms[1]
        repo.setEnabled(first.id, false)
        val vm = com.clockit.app.ui.MainViewModel(app)
        shoot("schedule") { com.clockit.app.ui.ScheduleScreen(repo.current, vm) }
    }

    @Test fun intervalSheet() = shoot("interval_sheet") { IntervalTemplateSheet(true, {}, { _, _, _ -> }) }

    @Test fun homeDisarmed() = shoot("home_disarmed") { HomeScreen(sample(false), {}, {}, {}) }
}
