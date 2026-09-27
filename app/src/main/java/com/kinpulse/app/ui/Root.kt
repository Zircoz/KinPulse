package com.kinpulse.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kinpulse.app.AppContainer
import com.kinpulse.app.data.SessionUser
import com.kinpulse.app.model.ReadingType
import com.kinpulse.app.ui.auth.AuthScreen
import com.kinpulse.app.ui.auth.VerifyEmailScreen
import com.kinpulse.app.ui.components.EmptyState
import com.kinpulse.app.ui.home.HomeScreen
import com.kinpulse.app.ui.profile.ProfileScreen
import com.kinpulse.app.ui.reading.ReadingEditScreen
import com.kinpulse.app.ui.sharing.MembersScreen

@Composable
fun KinPulseRoot(container: AppContainer) {
    val user by container.auth.user.collectAsStateWithLifecycle()
    val current = user
    when {
        current == null -> AuthScreen(container.auth)
        !current.emailVerified -> VerifyEmailScreen(container.auth, current)
        // Keyed by uid so switching accounts starts from a fresh back stack.
        else -> key(current.uid) { AppNavHost(container, current) }
    }
}

private object Routes {
    const val HOME = "home"
    const val PROFILE = "profile/{profileId}"
    const val READING = "profile/{profileId}/reading?type={type}&readingId={readingId}"
    const val MEMBERS = "profile/{profileId}/members"

    fun profile(id: String) = "profile/$id"
    fun newReading(profileId: String, type: ReadingType) = "profile/$profileId/reading?type=${type.name}"
    fun editReading(profileId: String, readingId: String) = "profile/$profileId/reading?readingId=$readingId"
    fun members(profileId: String) = "profile/$profileId/members"
}

@Composable
private fun AppNavHost(container: AppContainer, user: SessionUser) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                user = user,
                onOpenProfile = { nav.navigate(Routes.profile(it)) },
            )
        }
        composable(Routes.PROFILE) { entry ->
            val profileId = entry.arguments?.getString("profileId").orEmpty()
            ProfileScreen(
                container = container,
                user = user,
                profileId = profileId,
                onBack = { nav.popBackStack() },
                onAddReading = { type -> nav.navigate(Routes.newReading(profileId, type)) },
                onEditReading = { readingId -> nav.navigate(Routes.editReading(profileId, readingId)) },
                onOpenMembers = { nav.navigate(Routes.members(profileId)) },
            )
        }
        composable(
            Routes.READING,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("readingId") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val args = entry.arguments
            ReadingEditScreen(
                container = container,
                user = user,
                profileId = args?.getString("profileId").orEmpty(),
                initialType = args?.getString("type")?.let { ReadingType.valueOf(it) } ?: ReadingType.SUGAR,
                readingId = args?.getString("readingId"),
                onDone = { nav.popBackStack() },
            )
        }
        composable(Routes.MEMBERS) { entry ->
            MembersScreen(
                container = container,
                user = user,
                profileId = entry.arguments?.getString("profileId").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
fun NotConfiguredScreen() {
    EmptyState(
        title = "Firebase is not configured",
        body = "Add app/google-services.json from your Firebase project and rebuild. See README.md for the setup steps.",
    )
}
