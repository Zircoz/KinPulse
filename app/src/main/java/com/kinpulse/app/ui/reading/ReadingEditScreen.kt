package com.kinpulse.app.ui.reading

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.ReadingType
import com.kinpulse.app.model.SugarContext
import com.kinpulse.app.ui.components.LevelChip
import com.kinpulse.app.ui.components.LoadingBox
import com.kinpulse.app.ui.components.formatDate
import com.kinpulse.app.ui.components.formatTime
import com.kinpulse.app.ui.components.rememberViewModel
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadingEditScreen(
    container: AppContainer,
    user: SessionUser,
    profileId: String,
    initialType: ReadingType,
    readingId: String?,
    onDone: () -> Unit,
) {
    val vm = rememberViewModel { ReadingEditViewModel(container, user, profileId, readingId, initialType) }
    val snackbar = remember { SnackbarHostState() }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm.error) {
        vm.error?.let { snackbar.showSnackbar(it); vm.clearError() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Edit reading" else "New reading") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    if (vm.isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete reading")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (vm.loading) {
            LoadingBox(Modifier.padding(padding))
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!vm.isEditing) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = vm.type == ReadingType.SUGAR,
                            onClick = { vm.onTypeChange(ReadingType.SUGAR) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("Sugar") }
                        SegmentedButton(
                            selected = vm.type == ReadingType.BP,
                            onClick = { vm.onTypeChange(ReadingType.BP) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("Blood pressure") }
                    }
                }

                when (vm.type) {
                    ReadingType.SUGAR -> SugarFields(vm)
                    ReadingType.BP -> BpFields(vm)
                }

                vm.previewLevel?.let { level ->
                    Row(verticalAlignment = Alignment.CenterVertically) { LevelChip(level) }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("When", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  " + formatDate(vm.takenAt))
                        }
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text(formatTime(vm.takenAt))
                        }
                    }
                    vm.takenAtError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = vm.note,
                    onValueChange = vm::setNote,
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. after walk, missed dose") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = { if (vm.save()) onDone() },
                    enabled = vm.isValid,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this reading?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.delete(onDone)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val currentLocalDate = Instant.ofEpochMilli(vm.takenAt).atZone(zone).toLocalDate()
        val initialUtcMillis = currentLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    val picked = state.selectedDateMillis
                    if (picked != null) {
                        val pickedDate = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                        val existingTime = Instant.ofEpochMilli(vm.takenAt).atZone(zone).toLocalTime()
                        vm.onTakenAtChange(pickedDate.atTime(existingTime).atZone(zone).toInstant().toEpochMilli())
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val context = LocalContext.current
        val currentLocalTime = Instant.ofEpochMilli(vm.takenAt).atZone(zone).toLocalTime()
        val state = rememberTimePickerState(
            initialHour = currentLocalTime.hour,
            initialMinute = currentLocalTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    val currentLocalDate = Instant.ofEpochMilli(vm.takenAt).atZone(zone).toLocalDate()
                    val newTime = LocalTime.of(state.hour, state.minute)
                    vm.onTakenAtChange(currentLocalDate.atTime(newTime).atZone(zone).toInstant().toEpochMilli())
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SugarFields(vm: ReadingEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = vm.sugarText,
            onValueChange = vm::setSugarText,
            label = { Text("Blood sugar") },
            suffix = { Text("mg/dL") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = vm.sugarError != null,
            supportingText = vm.sugarError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SugarContext.entries.forEach { ctx ->
                FilterChip(
                    selected = vm.sugarContext == ctx,
                    onClick = { vm.onSugarContextChange(ctx) },
                    label = { Text(ctx.label) },
                )
            }
        }
    }
}

@Composable
private fun BpFields(vm: ReadingEditViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = vm.systolicText,
                onValueChange = vm::setSystolicText,
                label = { Text("Systolic") },
                suffix = { Text("mmHg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = vm.systolicError != null,
                supportingText = vm.systolicError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = vm.diastolicText,
                onValueChange = vm::setDiastolicText,
                label = { Text("Diastolic") },
                suffix = { Text("mmHg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = vm.diastolicError != null,
                supportingText = vm.diastolicError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = vm.pulseText,
            onValueChange = vm::setPulseText,
            label = { Text("Pulse (optional)") },
            suffix = { Text("bpm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = vm.pulseError != null,
            supportingText = vm.pulseError?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
