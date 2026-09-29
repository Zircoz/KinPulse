package com.kinpulse.app.data

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Gets a Google ID token through Android's Credential Manager, for [AuthRepository.signInWithGoogle]. */
object GoogleSignIn {

    /**
     * The OAuth web client id that the google-services plugin generates from google-services.json.
     * Looked up by name because the resource only exists once Google sign-in is enabled in Firebase;
     * null means the Google button should be hidden.
     */
    @SuppressLint("DiscouragedApi")
    fun webClientId(context: Context): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id)
    }

    /** Shows the Google account picker. Returns null if the user dismisses it. */
    suspend fun requestIdToken(activityContext: Context, webClientId: String): String? {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            return null
        }
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unexpected credential type: ${credential.type}")
    }
}
