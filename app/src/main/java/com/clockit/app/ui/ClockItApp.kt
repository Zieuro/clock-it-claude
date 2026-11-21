package com.clockit.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockit.app.ui.theme.Bg
import com.clockit.app.ui.theme.Blue
import com.clockit.app.ui.theme.Surface1
import com.clockit.app.ui.theme.TextLow

@Composable
fun ClockItApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Surface1) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue, selectedTextColor = Blue,
                    indicatorColor = Blue.copy(alpha = 0.15f),
                    unselectedIconColor = TextLow, unselectedTextColor = TextLow,
                )
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Rounded.Timer, null) }, label = { Text("Countdown") }, colors = colors)
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, null) }, label = { Text("Schedule") }, colors = colors)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                when (t) {
                    0 -> HomeScreen(
                        state = state,
                        onArm = vm::setArmed,
                        onOpenSchedule = { tab = 1 },
                        onPermissionChange = vm::resync,
                    )
                    else -> ScheduleScreen(state, vm)
                }
            }
        }
    }
}
