package com.kinpulse.app.ui.sharing

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.Invite
import com.kinpulse.app.model.Profile
import com.kinpulse.app.model.Role
import com.kinpulse.app.ui.components.LoadingBox
import com.kinpulse.app.ui.components.rememberViewModel
import com.kinpulse.app.ui.profile.ProfileState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(container: AppContainer, user: SessionUser, profileId: String, onBack: () -> Unit) {
    val vm = rememberViewModel { MembersViewModel(container, user, profileId) }
    val state by vm.profile.collectAsStateWithLifecycle()
    val pendingInvites by vm.pendingInvites.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val info by vm.info.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        if (state is ProfileState.Gone) onBack()
    }
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); vm.error.value = null }
    }
    LaunchedEffect(info) {
        info?.let { snackbar.showSnackbar(it); vm.info.value = null }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family access") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val current = state) {
            is ProfileState.Loading, ProfileState.Gone -> LoadingBox(Modifier.padding(padding))
            is ProfileState.Ready -> MembersContent(
                modifier = Modifier.padding(padding),
                profile = current.profile,
                user = user,
                pendingInvites = pendingInvites,
                onChangeRole = vm::changeRole,
                onRemoveMember = vm::removeMember,
                onCancelInvite = vm::cancelInvite,
                onInvite = vm::invite,
            )
        }
    }
}

@Composable
private fun MembersContent(
    modifier: Modifier,
    profile: Profile,
    user: SessionUser,
    pendingInvites: List<Invite>,
    onChangeRole: (String, Role) -> Unit,
    onRemoveMember: (String) -> Unit,
    onCancelInvite: (String) -> Unit,
    onInvite: (String, Role, () -> Unit) -> Unit,
) {
    val isOwner = profile.roleOf(user.uid) == Role.OWNER
    val members = profile.roles.entries.sortedWith(
        compareBy({ it.value != Role.OWNER }, { (profile.memberNames[it.key] ?: "").lowercase() }),
    )

    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Choose who can see ${profile.name}'s readings.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item { SectionTitle("People with access") }
        items(members, key = { it.key }) { (uid, role) ->
            MemberRow(
                name = profile.memberNames[uid] ?: "Someone",
                isSelf = uid == user.uid,
                role = role,
                canManage = isOwner && role != Role.OWNER,
                onChangeRole = { newRole -> onChangeRole(uid, newRole) },
                onRemove = { onRemoveMember(uid) },
            )
        }

        if (isOwner) {
            item { SectionTitle("Invite a family member") }
            item {
                InviteCard(profileName = profile.name, onInvite = onInvite)
            }
            if (pendingInvites.isNotEmpty()) {
                item { SectionTitle("Pending invites") }
                items(pendingInvites, key = { it.email }) { invite ->
                    Card(Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(invite.email) },
                            supportingContent = { Text(invite.role.label) },
                            trailingContent = {
                                IconButton(onClick = { onCancelInvite(invite.email) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel invite")
                                }
                            },
                        )
                    }
                }
            }
        } else {
            item {
                val ownerName = profile.memberNames[profile.ownerId] ?: "the owner"
                Text(
                    "Only $ownerName, the owner, can invite or remove people or change what they can do.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun MemberRow(
    name: String,
    isSelf: Boolean,
    role: Role,
    canManage: Boolean,
    onChangeRole: (Role) -> Unit,
    onRemove: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(if (isSelf) "$name (you)" else name) },
            supportingContent = { Text(role.label) },
            trailingContent = if (canManage) {
                {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Manage access")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(Role.EDITOR.label) },
                            leadingIcon = if (role == Role.EDITOR) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            onClick = { menuExpanded = false; onChangeRole(Role.EDITOR) },
                        )
                        DropdownMenuItem(
                            text = { Text(Role.VIEWER.label) },
                            leadingIcon = if (role == Role.VIEWER) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            onClick = { menuExpanded = false; onChangeRole(Role.VIEWER) },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove access") },
                            onClick = { menuExpanded = false; confirmRemove = true },
                        )
                    }
                }
            } else null,
        )
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove access?") },
            text = { Text("$name will no longer be able to see or add readings for this person.") },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteCard(profileName: String, onInvite: (String, Role, () -> Unit) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current
    val roleOptions = listOf(Role.EDITOR, Role.VIEWER)
    val role = roleOptions[selectedIndex]

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                roleOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = roleOptions.size),
                        onClick = { selectedIndex = index },
                        selected = index == selectedIndex,
                        label = { Text(option.label) },
                    )
                }
            }
            Button(
                onClick = { onInvite(email, role) { email = "" } },
                enabled = email.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Send invite") }
            Text(
                "They need to install KinPulse and sign up with this email address. The invite will appear " +
                    "on their home screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    val text = "I've added you to $profileName's health readings on KinPulse. " +
                        "Install the app and sign up with $email to see them."
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, "Tell them"))
                },
                enabled = email.isNotBlank(),
            ) { Text("Tell them") }
        }
    }
}
