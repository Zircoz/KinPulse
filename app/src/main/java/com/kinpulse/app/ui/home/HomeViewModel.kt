package com.kinpulse.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.Invite
import com.kinpulse.app.model.Profile
import com.kinpulse.app.ui.components.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val container: AppContainer, private val user: SessionUser) : ViewModel() {

    val error = MutableStateFlow<String?>(null)

    /** Null while loading. */
    val profiles: StateFlow<List<Profile>?> = container.health.profilesFor(user.uid)
        .catch { error.value = it.userMessage(); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val invites: StateFlow<List<Invite>> = container.health.invitesForEmail(user.email)
        .catch { error.value = it.userMessage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createProfile(name: String): String = container.health.createProfile(name, user)

    fun accept(invite: Invite) = launch { container.health.acceptInvite(invite, user) }

    fun decline(invite: Invite) = launch { container.health.cancelInvite(invite.profileId, invite.email) }

    fun signOut() = container.auth.signOut()

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
