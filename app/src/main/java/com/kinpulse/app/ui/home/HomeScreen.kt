package com.kinpulse.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.Invite
import com.kinpulse.app.model.Profile
import com.kinpulse.app.model.Role
import com.kinpulse.app.ui.components.EmptyState
import com.kinpulse.app.ui.components.LoadingBox
import com.kinpulse.app.ui.components.rememberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: AppContainer, user: SessionUser, onOpenProfile: (String) -> Unit) {
    val vm = rememberViewModel { HomeViewModel(container, user) }
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val invites by vm.invites.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); vm.error.value = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KinPulse") },
                actions = {
                    IconButton(onClick = vm::signOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign out")
                    }
                },
            )
        },
        floatingActionButton = {
            if (!profiles.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add person") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = profiles
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() && invites.isEmpty() -> EmptyState(
                title = "Welcome, ${user.name}",
                body = "Create a profile for yourself or a family member to start recording sugar and blood pressure. " +
                    "If someone invited you, their invite will appear here.",
                modifier = Modifier.padding(padding),
            ) {
                Button(onClick = { showCreate = true }) { Text("Create a profile") }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (invites.isNotEmpty()) {
                    item { SectionTitle("Invitations") }
                    items(invites, key = { "invite-${it.profileId}" }) { invite ->
                        InviteCard(invite, onAccept = { vm.accept(invite) }, onDecline = { vm.decline(invite) })
                    }
                }
                if (list.isNotEmpty()) {
                    item { SectionTitle("People") }
                    items(list, key = { it.id }) { profile ->
                        ProfileCard(profile, user, onClick = { onOpenProfile(profile.id) })
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateProfileDialog(
            suggestedName = if (profiles.isNullOrEmpty()) user.name else "",
            onDismiss = { showCreate = false },
            onCreate = { name ->
                showCreate = false
                onOpenProfile(vm.createProfile(name))
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ProfileCard(profile: Profile, user: SessionUser, onClick: () -> Unit) {
    val role = profile.roleOf(user.uid)
    val subtitle = when (role) {
        Role.OWNER -> {
            val others = profile.roles.size - 1
            if (others == 0) "Only you" else "Shared with $others ${if (others == 1) "person" else "people"}"
        }
        else -> "Shared by ${profile.memberNames[profile.ownerId] ?: "a family member"} · ${role?.label.orEmpty()}"
    }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        ListItem(
            headlineContent = { Text(profile.name, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
        )
    }
}

@Composable
private fun InviteCard(invite: Invite, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${invite.invitedByName.ifBlank { "Someone" }} invited you to ${invite.profileName}'s readings",
                style = MaterialTheme.typography.titleSmall,
            )
            Text("Access: ${invite.role.label}", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept) { Text("Accept") }
                OutlinedButton(onClick = onDecline) { Text("Decline") }
            }
        }
    }
}

@Composable
private fun CreateProfileDialog(suggestedName: String, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(suggestedName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Whose readings?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create a profile for yourself or for a family member, e.g. \"Dad\" or \"Grandma\".")
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
