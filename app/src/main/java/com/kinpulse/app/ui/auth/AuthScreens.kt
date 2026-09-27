package com.kinpulse.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kinpulse.app.data.AuthRepository
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.ui.components.userMessage
import kotlinx.coroutines.launch

private enum class AuthMode { SIGN_IN, SIGN_UP, RESET }

@Composable
fun AuthScreen(auth: AuthRepository) {
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun run(block: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                message = e.userMessage()
            } finally {
                busy = false
            }
        }
    }

    val canSubmit = !busy && email.contains('@') && when (mode) {
        AuthMode.SIGN_IN -> password.isNotEmpty()
        AuthMode.SIGN_UP -> name.isNotBlank() && password.length >= 6
        AuthMode.RESET -> true
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("KinPulse", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text(
                "Sugar and blood pressure for the whole family, in one place.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                when (mode) {
                    AuthMode.SIGN_IN -> "Sign in"
                    AuthMode.SIGN_UP -> "Create an account"
                    AuthMode.RESET -> "Reset password"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 24.dp),
            )
            if (mode == AuthMode.SIGN_UP) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            if (mode != AuthMode.RESET) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (mode == AuthMode.SIGN_UP) "Password (6+ characters)" else "Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Button(
                onClick = {
                    when (mode) {
                        AuthMode.SIGN_IN -> run { auth.signIn(email, password) }
                        AuthMode.SIGN_UP -> run { auth.signUp(name, email, password) }
                        AuthMode.RESET -> run {
                            auth.sendPasswordReset(email)
                            mode = AuthMode.SIGN_IN
                            message = "Password reset email sent to $email"
                        }
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        when (mode) {
                            AuthMode.SIGN_IN -> "Sign in"
                            AuthMode.SIGN_UP -> "Create account"
                            AuthMode.RESET -> "Send reset link"
                        },
                    )
                }
            }
            when (mode) {
                AuthMode.SIGN_IN -> {
                    TextButton(onClick = { mode = AuthMode.SIGN_UP; message = null }) { Text("New here? Create an account") }
                    TextButton(onClick = { mode = AuthMode.RESET; message = null }) { Text("Forgot password?") }
                }
                else -> TextButton(onClick = { mode = AuthMode.SIGN_IN; message = null }) { Text("Back to sign in") }
            }
        }
    }
}

@Composable
fun VerifyEmailScreen(auth: AuthRepository, user: SessionUser) {
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun run(block: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                message = e.userMessage()
            } finally {
                busy = false
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Confirm your email", style = MaterialTheme.typography.headlineSmall)
            Text(
                "We sent a link to ${user.email}. Open it, then come back and tap the button below. " +
                    "Confirming your email lets family members share readings with you safely.",
                textAlign = TextAlign.Center,
            )
            message?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Button(
                onClick = {
                    run {
                        auth.refresh()
                        if (auth.user.value?.emailVerified != true) message = "Not confirmed yet. Check your inbox (and spam)."
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("I've confirmed my email") }
            OutlinedButton(
                onClick = { run { auth.sendVerificationEmail(); message = "Sent again to ${user.email}" } },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Resend email") }
            TextButton(onClick = { auth.signOut() }) { Text("Use a different account") }
        }
    }
}
