package com.kinpulse.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class SessionUser(
    val uid: String,
    val email: String,
    val name: String,
    val emailVerified: Boolean,
)

private fun FirebaseUser.toSessionUser() = SessionUser(
    uid = uid,
    email = email.orEmpty().lowercase(),
    name = displayName?.takeIf { it.isNotBlank() } ?: email.orEmpty().substringBefore('@'),
    emailVerified = isEmailVerified,
)

class AuthRepository(private val auth: FirebaseAuth) {

    private val _user = MutableStateFlow(auth.currentUser?.toSessionUser())

    /** Signed-in user; also updated after profile changes and reloads, which the auth listener misses. */
    val user: StateFlow<SessionUser?> = _user.asStateFlow()

    init {
        auth.addAuthStateListener { _user.value = it.currentUser?.toSessionUser() }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUp(name: String, email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user ?: return
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        user.sendEmailVerification().await()
        _user.value = auth.currentUser?.toSessionUser()
    }

    suspend fun sendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    /**
     * Reloads the user after they click the verification link. The ID token is refreshed too,
     * because Firestore rules read `email_verified` from the token, not from the user record.
     */
    suspend fun refresh() {
        val user = auth.currentUser ?: return
        user.reload().await()
        user.getIdToken(true).await()
        _user.value = auth.currentUser?.toSessionUser()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() = auth.signOut()
}
