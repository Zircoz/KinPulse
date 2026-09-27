package com.kinpulse.app.ui.sharing

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.Invite
import com.kinpulse.app.model.Role
import com.kinpulse.app.ui.components.userMessage
import com.kinpulse.app.ui.profile.ProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MembersViewModel(
    private val container: AppContainer,
    private val user: SessionUser,
    private val profileId: String,
) : ViewModel() {

    val error = MutableStateFlow<String?>(null)
    val info = MutableStateFlow<String?>(null)

    val profile: StateFlow<ProfileState> = container.health.profile(profileId)
        .map { profile ->
            if (profile == null || profile.roleOf(user.uid) == null) ProfileState.Gone else ProfileState.Ready(profile)
        }
        .catch { emit(ProfileState.Gone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState.Loading)

    /**
     * The security rules let any member read a profile's invites (not just the owner), but only
     * the owner's UI shows them — a non-owner failing to read this (e.g. offline) is harmless.
     */
    val pendingInvites: StateFlow<List<Invite>> = container.health.pendingInvites(profileId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun invite(email: String, role: Role, onSuccess: () -> Unit) = launch {
        val trimmed = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            error.value = "Enter a valid email address"
            return@launch
        }
        if (trimmed.lowercase() == user.email.lowercase()) {
            error.value = "You can't invite yourself"
            return@launch
        }
        val current = profile.value
        if (current !is ProfileState.Ready) {
            error.value = "Profile not loaded yet"
            return@launch
        }
        container.health.invite(current.profile, trimmed, role, user)
        info.value = "Invite sent to $trimmed"
        onSuccess()
    }

    fun cancelInvite(email: String) = launch {
        container.health.cancelInvite(profileId, email)
    }

    fun changeRole(uid: String, role: Role) = launch {
        container.health.changeRole(profileId, uid, role)
    }

    fun removeMember(uid: String) = launch {
        container.health.removeMember(profileId, uid)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                error.value = e.userMessage()
            }
        }
    }
}
