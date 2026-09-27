package com.kinpulse.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.Profile
import com.kinpulse.app.model.Reading
import com.kinpulse.app.ui.components.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileState {
    data object Loading : ProfileState
    /** Deleted, or the user no longer has access. */
    data object Gone : ProfileState
    data class Ready(val profile: Profile) : ProfileState
}

class ProfileViewModel(
    private val container: AppContainer,
    private val user: SessionUser,
    private val profileId: String,
) : ViewModel() {

    val error = MutableStateFlow<String?>(null)

    val state: StateFlow<ProfileState> = container.health.profile(profileId)
        .map { profile ->
            if (profile == null || profile.roleOf(user.uid) == null) ProfileState.Gone else ProfileState.Ready(profile)
        }
        .catch { emit(ProfileState.Gone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState.Loading)

    /** Newest first; null while loading. */
    val readings: StateFlow<List<Reading>?> = container.health.readings(profileId)
        .catch { error.value = it.userMessage(); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun rename(name: String) = launch { container.health.renameProfile(profileId, name) }

    fun delete(onDone: () -> Unit) = launch {
        container.health.deleteProfile(profileId)
        onDone()
    }

    fun leave(onDone: () -> Unit) = launch {
        container.health.removeMember(profileId, user.uid)
        onDone()
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
