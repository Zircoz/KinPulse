package com.kinpulse.app.ui.profile

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.Csv
import com.kinpulse.app.model.HealthRanges
import com.kinpulse.app.model.Reading
import com.kinpulse.app.model.ReadingType
import com.kinpulse.app.model.Role
import com.kinpulse.app.ui.components.EmptyState
import com.kinpulse.app.ui.components.LevelChip
import com.kinpulse.app.ui.components.LoadingBox
import com.kinpulse.app.ui.components.formatDateTime
import com.kinpulse.app.ui.components.formatTime
import com.kinpulse.app.ui.components.friendlyDay
import com.kinpulse.app.ui.components.rememberViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    container: AppContainer,
    user: SessionUser,
    profileId: String,
    onBack: () -> Unit,
    onAddReading: (ReadingType) -> Unit,
    onEditReading: (String) -> Unit,
    onOpenMembers: () -> Unit,
) {
    val vm = rememberViewModel { ProfileViewModel(container, user, profileId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val readings by vm.readings.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); vm.error.value = null }
    }

    when (val s = state) {
        ProfileState.Loading -> LoadingBox()
        ProfileState.Gone -> LaunchedEffect(Unit) { onBack() }
        is ProfileState.Ready -> ProfileContent(
            profile = s.profile,
            user = user,
            readings = readings ?: emptyList(),
            vm = vm,
            snackbar = snackbar,
            onBack = onBack,
            onAddReading = onAddReading,
            onEditReading = onEditReading,
            onOpenMembers = onOpenMembers,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    profile: com.kinpulse.app.model.Profile,
    user: SessionUser,
    readings: List<Reading>,
    vm: ProfileViewModel,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onAddReading: (ReadingType) -> Unit,
    onEditReading: (String) -> Unit,
    onOpenMembers: () -> Unit,
) {
    val context = LocalContext.current
    val role = profile.roleOf(user.uid)
    val canEdit = role?.canEdit == true
    val isOwner = role == Role.OWNER

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val selectedType = if (selectedTab == 0) ReadingType.SUGAR else ReadingType.BP
    val filtered = remember(readings, selectedType) { readings.filter { it.type == selectedType } }

    var menuExpanded by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMembers) {
                        Icon(Icons.Default.Share, contentDescription = "Family access")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = {
                                    menuExpanded = false
                                    val csv = Csv.export(readings)
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "${profile.name} – KinPulse readings")
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                    }
                                    context.startActivity(Intent.createChooser(send, "Export CSV"))
                                },
                            )
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = { menuExpanded = false; showRename = true },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete profile") },
                                    onClick = { menuExpanded = false; showDeleteConfirm = true },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Leave") },
                                    onClick = { menuExpanded = false; showLeaveConfirm = true },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (canEdit) {
                ExtendedFloatingActionButton(
                    onClick = { onAddReading(selectedType) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (selectedType == ReadingType.SUGAR) "Add sugar" else "Add BP") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (role == Role.VIEWER) {
                val ownerName = profile.memberNames[profile.ownerId] ?: "the owner"
                ViewerBanner(ownerName)
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Sugar") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Blood pressure") })
            }

            if (filtered.isEmpty()) {
                val typeLabel = if (selectedType == ReadingType.SUGAR) "sugar" else "blood pressure"
                EmptyState(
                    title = "No $typeLabel readings yet",
                    body = if (canEdit) "Tap + to add one." else "Ask an editor to add one.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                ReadingsList(
                    filtered = filtered,
                    type = selectedType,
                    canEdit = canEdit,
                    onEditReading = onEditReading,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showRename) {
        RenameDialog(
            initialName = profile.name,
            onDismiss = { showRename = false },
            onConfirm = { name -> showRename = false; vm.rename(name) },
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${profile.name}'s profile?") },
            text = { Text("This permanently deletes all readings for everyone who has access. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; vm.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave ${profile.name}'s profile?") },
            text = { Text("You'll lose access to these readings unless someone invites you again.") },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; vm.leave(onBack) }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ViewerBanner(ownerName: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "You can view only. Ask $ownerName for edit access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RenameDialog(initialName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename profile") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private data class PeriodAvg(val label: String, val text: String)

private fun computeAverages(filtered: List<Reading>, type: ReadingType): List<PeriodAvg> {
    val now = System.currentTimeMillis()
    val periods = listOf(7 to "7-day avg", 30 to "30-day avg")
    return periods.mapNotNull { (days, label) ->
        val cutoff = now - days * 24L * 60 * 60 * 1000
        val inPeriod = filtered.filter { it.takenAt >= cutoff }
        if (inPeriod.isEmpty()) return@mapNotNull null
        val text = when (type) {
            ReadingType.SUGAR -> {
                val avg = inPeriod.mapNotNull { it.sugarMgDl }.average()
                "${avg.roundToInt()} mg/dL"
            }
            ReadingType.BP -> {
                val avgSys = inPeriod.mapNotNull { it.systolic }.average()
                val avgDia = inPeriod.mapNotNull { it.diastolic }.average()
                "${avgSys.roundToInt()}/${avgDia.roundToInt()} mmHg"
            }
        }
        PeriodAvg(label, text)
    }
}

@Composable
private fun ReadingsList(
    filtered: List<Reading>,
    type: ReadingType,
    canEdit: Boolean,
    onEditReading: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest = filtered.first()
    val averages = remember(filtered, type) { computeAverages(filtered, type) }
    val grouped = remember(filtered) { filtered.groupBy { friendlyDay(it.takenAt) } }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "latest") { LatestCard(latest, type) }
        item(key = "averages") { AveragesCard(averages) }
        item(key = "trend") { TrendCard(filtered, type) }

        grouped.forEach { (day, dayReadings) ->
            item(key = "header-$day") {
                Text(
                    day,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(dayReadings, key = { it.id }) { reading ->
                ReadingRow(reading, canEdit, onEditReading)
            }
        }

        item(key = "footer") {
            Text(
                "Ranges are a general guide for adults, not medical advice.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LatestCard(latest: Reading, type: ReadingType) {
    val level = HealthRanges.of(latest)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Latest", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val mainValue = when (type) {
                    ReadingType.SUGAR -> "${latest.sugarMgDl} mg/dL"
                    ReadingType.BP -> "${latest.systolic}/${latest.diastolic} mmHg"
                }
                Text(mainValue, style = MaterialTheme.typography.headlineLarge)
                when (type) {
                    ReadingType.SUGAR -> latest.sugarMgDl?.let { mgDl ->
                        Text(
                            "(${"%.1f".format(HealthRanges.toMmol(mgDl))} mmol/L)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ReadingType.BP -> latest.pulse?.let { pulse ->
                        Text(
                            "Pulse $pulse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            level?.let { LevelChip(it) }
            if (type == ReadingType.SUGAR) {
                latest.sugarContext?.let {
                    Text(it.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                formatDateTime(latest.takenAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Added by ${latest.addedByName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AveragesCard(averages: List<PeriodAvg>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Averages", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (averages.isEmpty()) {
                Text(
                    "Not enough recent data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    averages.forEach { avg ->
                        Column {
                            Text(
                                avg.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(avg.text, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendCard(filtered: List<Reading>, type: ReadingType) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Trend", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            TrendChart(filtered, type, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReadingRow(reading: Reading, canEdit: Boolean, onEditReading: (String) -> Unit) {
    val level = HealthRanges.of(reading)
    val headline = when (reading.type) {
        ReadingType.SUGAR -> "${reading.sugarMgDl} mg/dL"
        ReadingType.BP -> "${reading.systolic}/${reading.diastolic} mmHg"
    }
    val detailParts = buildList {
        add(formatTime(reading.takenAt))
        when (reading.type) {
            ReadingType.SUGAR -> reading.sugarContext?.let { add(it.label) }
            ReadingType.BP -> reading.pulse?.let { add("Pulse $it") }
        }
        if (reading.note.isNotBlank()) add(reading.note)
        if (reading.addedByName.isNotBlank()) add("by ${reading.addedByName}")
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        ListItem(
            headlineContent = { Text(headline) },
            supportingContent = { Text(detailParts.joinToString(" · ")) },
            trailingContent = { level?.let { LevelChip(it) } },
            modifier = if (canEdit) Modifier.clickable { onEditReading(reading.id) } else Modifier,
        )
    }
}
