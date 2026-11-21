package com.clockit.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.clockit.app.alarm.formatClock
import com.clockit.app.data.Alarm
import com.clockit.app.data.IntervalTemplate
import com.clockit.app.ui.components.TimePickerDialog
import com.clockit.app.ui.theme.Blue
import com.clockit.app.ui.theme.Danger
import com.clockit.app.ui.theme.Outline
import com.clockit.app.ui.theme.Surface1
import com.clockit.app.ui.theme.Surface2
import com.clockit.app.ui.theme.TextHigh
import com.clockit.app.ui.theme.TextLow
import com.clockit.app.ui.theme.TextMid

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Blue, unfocusedBorderColor = Outline,
    focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
)

/** Create or edit a single alarm: time + optional title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorDialog(alarm: Alarm, onDismiss: () -> Unit, onSave: (Alarm) -> Unit, onDelete: (() -> Unit)?) {
    val context = LocalContext.current
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var title by remember { mutableStateOf(alarm.title) }
    var picking by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = Surface1) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    if (alarm.id == 0) "New alarm" else "Edit alarm",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(20.dp))
                val (time, period) = splitClock(context, hour, minute)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface2)
                        .clickable { picking = true }
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(time, style = MaterialTheme.typography.displayLarge, color = TextHigh)
                    if (period.isNotEmpty()) {
                        Text(" $period", style = MaterialTheme.typography.titleLarge, color = Blue, modifier = Modifier.padding(bottom = 10.dp))
                    }
                }
                Text(
                    "Tap the time to change it", style = MaterialTheme.typography.bodySmall, color = TextLow,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) { Text("Delete", color = Danger) }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { onSave(alarm.copy(hour = hour, minute = minute, title = title.trim())) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
    if (picking) {
        TimePickerDialog("Alarm time", hour, minute, onDismiss = { picking = false }) { h, m ->
            hour = h; minute = m; picking = false
        }
    }
}

/** Template that fills the schedule with alarms every N minutes between a start and end time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalTemplateSheet(
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onApply: (times: List<Int>, title: String, replace: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var start by rememberSaveable { mutableIntStateOf(7 * 60) }
    var end by rememberSaveable { mutableIntStateOf(9 * 60) }
    var intervalText by rememberSaveable { mutableStateOf("15") }
    var title by rememberSaveable { mutableStateOf("") }
    var replace by rememberSaveable { mutableStateOf(true) }
    var picking by remember { mutableStateOf<String?>(null) }

    val interval = intervalText.toIntOrNull() ?: 0
    val times = remember(start, end, interval) { IntervalTemplate.generate(start, end, interval) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Surface1) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Interval template", style = MaterialTheme.typography.titleLarge)
            Text(
                "Generates alarms you can edit individually afterwards.",
                style = MaterialTheme.typography.bodyMedium, color = TextMid,
            )
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeTile("Start", formatClock(context, start / 60, start % 60), Modifier.weight(1f)) { picking = "start" }
                TimeTile("End", formatClock(context, end / 60, end % 60), Modifier.weight(1f)) { picking = "end" }
            }
            if (end < start) {
                Text(
                    "Ends the next day", style = MaterialTheme.typography.bodySmall, color = Blue,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Every", style = MaterialTheme.typography.labelLarge, color = TextMid)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { intervalText = (interval - 5).coerceAtLeast(1).toString() }) {
                    Icon(Icons.Rounded.Remove, "Decrease")
                }
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { v -> intervalText = v.filter { it.isDigit() }.take(4) },
                    singleLine = true,
                    suffix = { Text("min") },
                    colors = fieldColors(),
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = { intervalText = (interval + 5).coerceAtMost(1440).toString() }) {
                    Icon(Icons.Rounded.Add, "Increase")
                }
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30, 60).forEach { preset ->
                    val selected = interval == preset
                    Text(
                        if (preset == 60) "1 h" else "$preset m",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else TextMid,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Blue else Surface2)
                            .clickable { intervalText = preset.toString() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(60) },
                label = { Text("Title for these alarms (optional)") },
                singleLine = true,
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (hasExisting) {
                Spacer(Modifier.height(16.dp))
                val segColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Blue.copy(alpha = 0.2f), activeContentColor = TextHigh,
                    inactiveContainerColor = Surface2, inactiveContentColor = TextMid,
                    activeBorderColor = Outline, inactiveBorderColor = Outline,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(replace, { replace = true }, SegmentedButtonDefaults.itemShape(0, 2), colors = segColors) { Text("Replace schedule") }
                    SegmentedButton(!replace, { replace = false }, SegmentedButtonDefaults.itemShape(1, 2), colors = segColors) { Text("Add to schedule") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(16.dp)).padding(16.dp),
            ) {
                Text(
                    if (times.isEmpty()) "Enter an interval of at least 1 minute"
                    else "${times.size} alarm${if (times.size == 1) "" else "s"}" +
                        if (times.size >= IntervalTemplate.MAX_ALARMS) " (max)" else "",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (times.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val preview = times.take(6).joinToString("  ·  ") { formatClock(context, it / 60, it % 60) }
                    Text(
                        preview + if (times.size > 6) "  …" else "",
                        style = MaterialTheme.typography.bodySmall, color = TextMid,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onApply(times, title, replace || !hasExisting) },
                enabled = times.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text("Create alarms") }
        }
    }

    picking?.let { which ->
        val value = if (which == "start") start else end
        TimePickerDialog(
            if (which == "start") "Start time" else "End time", value / 60, value % 60,
            onDismiss = { picking = null },
        ) { h, m ->
            if (which == "start") start = h * 60 + m else end = h * 60 + m
            picking = null
        }
    }
}

@Composable
private fun TimeTile(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextLow)
        Spacer(Modifier.size(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = TextHigh)
    }
}
