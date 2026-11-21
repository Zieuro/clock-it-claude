package com.clockit.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clockit.app.data.Alarm
import com.clockit.app.data.ScheduleState
import com.clockit.app.ui.theme.Blue
import com.clockit.app.ui.theme.Danger
import com.clockit.app.ui.theme.Outline
import com.clockit.app.ui.theme.Surface1
import com.clockit.app.ui.theme.TextHigh
import com.clockit.app.ui.theme.TextLow
import com.clockit.app.ui.theme.TextMid
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(state: ScheduleState, vm: MainViewModel) {
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var showTemplate by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Schedule", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${state.alarms.size} alarms · ${state.activeAlarms.size} active",
                    style = MaterialTheme.typography.bodyMedium, color = TextMid,
                )
            }
            if (state.alarms.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, "Clear schedule", tint = TextMid)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    val t = LocalTime.now().plusMinutes(1)
                    editing = Alarm(id = 0, hour = t.hour, minute = t.minute)
                },
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add alarm")
            }
            FilledTonalButton(
                onClick = { showTemplate = true },
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(Icons.Rounded.Timelapse, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Interval")
            }
        }

        if (state.alarms.isEmpty()) {
            EmptySchedule()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.alarms, key = { it.id }) { alarm ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) { vm.delete(alarm.id); true } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        modifier = Modifier.animateItem(),
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Danger.copy(alpha = 0.2f))
                                    .padding(end = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = Danger) }
                        },
                    ) {
                        AlarmRow(alarm, state.armed, onClick = { editing = alarm }, onToggle = { vm.setEnabled(alarm.id, it) })
                    }
                }
            }
        }
    }

    editing?.let { alarm ->
        AlarmEditorDialog(
            alarm = alarm,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = if (alarm.id != 0) ({ vm.delete(alarm.id); editing = null }) else null,
        )
    }
    if (showTemplate) {
        IntervalTemplateSheet(
            hasExisting = state.alarms.isNotEmpty(),
            onDismiss = { showTemplate = false },
            onApply = { times, title, replace -> vm.applyTemplate(times, title, replace); showTemplate = false },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Surface1,
            title = { Text("Clear schedule?") },
            text = { Text("All ${state.alarms.size} alarms will be removed.") },
            confirmButton = {
                TextButton(onClick = { vm.clear(); confirmClear = false }) { Text("Clear", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, armed: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val live = alarm.enabled
    val (time, period) = splitClock(context, alarm.hour, alarm.minute)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface1)
            .border(1.dp, if (live && armed) Blue.copy(alpha = 0.25f) else Outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    time, style = MaterialTheme.typography.displaySmall,
                    color = if (live) TextHigh else TextLow,
                )
                if (period.isNotEmpty()) {
                    Text(
                        " $period", style = MaterialTheme.typography.titleMedium,
                        color = if (live) TextMid else TextLow, modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
            }
            Text(
                alarm.title.ifBlank { "No title" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (alarm.title.isBlank()) TextLow else if (live) TextMid else TextLow,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Blue,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = Outline,
                uncheckedThumbColor = TextMid,
            ),
        )
    }
}

@Composable
private fun EmptySchedule() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Timelapse, null, tint = TextLow, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Your schedule is empty", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Add alarms one at a time, or use an interval template to fill a time window in one go.",
            style = MaterialTheme.typography.bodyMedium, color = TextMid,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

fun splitClock(context: android.content.Context, hour: Int, minute: Int): Pair<String, String> =
    if (android.text.format.DateFormat.is24HourFormat(context)) "%02d:%02d".format(hour, minute) to ""
    else "%d:%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute) to (if (hour < 12) "AM" else "PM")
