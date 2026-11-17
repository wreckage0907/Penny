package com.penny.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.penny.app.PennyServices
import com.penny.app.core.AppRoutes
import com.penny.app.core.AuthFlows
import kotlinx.coroutines.launch

/**
 * MaterialApp equivalent (main.dart:37-58): AuthWrapper at the root plus the
 * Flutter named routes. Routes whose screens are not migrated yet render
 * [NotMigratedScreen]; each later slice swaps in the real screen.
 */
@Composable
fun PennyApp(services: PennyServices, nav: NavHostController = rememberNavController()) {
    PennyTheme {
        // targetSdk 36 is edge-to-edge: keep content clear of the system bars.
        Surface(Modifier.fillMaxSize()) {
          Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            NavHost(nav, startDestination = AppRoutes.navRoute(AppRoutes.ROOT)) {
                composable(AppRoutes.navRoute(AppRoutes.ROOT)) { AuthWrapper(services, nav) }
                composable(AppRoutes.navRoute(AppRoutes.LOGIN)) { LoginScreen(services, authNav(nav)) }
                composable(AppRoutes.navRoute(AppRoutes.SIGNUP)) { SignUpScreen(services, authNav(nav)) }
                composable(AppRoutes.navRoute(AppRoutes.ONBOARDING)) { OnboardingScreen(services, authNav(nav)) }
                composable(AppRoutes.navRoute(AppRoutes.HOME)) { HomeScreen(services, navigateNamed(nav)) }
                composable(AppRoutes.navRoute(AppRoutes.COURSE_PAGE)) {
                    CourseListScreen(services, onBack = { nav.popBackStack() }) { folder ->
                        nav.navigate("learning/${Uri.encode(folder)}")
                    }
                }
                // MaterialPageRoute pushes in Flutter (no route names): internal routes.
                composable("learning/{subfolder}") { entry ->
                    val folder = entry.arguments?.getString("subfolder").orEmpty()
                    LearningScreen(folder, onBack = { nav.popBackStack() }) { index ->
                        nav.navigate("lesson/${Uri.encode(folder)}/$index")
                    }
                }
                composable("lesson/{subfolder}/{index}") { entry ->
                    LessonScreen(
                        services,
                        entry.arguments?.getString("subfolder").orEmpty(),
                        entry.arguments?.getString("index")?.toIntOrNull() ?: 0,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(AppRoutes.navRoute(AppRoutes.PRACTICE)) {
                    PracticeListScreen(onBack = { nav.popBackStack() }) { index -> nav.navigate("mcq/$index") }
                }
                composable("mcq/{index}") { entry ->
                    McqScreen(services, entry.arguments?.getString("index")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() })
                }
                composable(AppRoutes.navRoute(AppRoutes.BUDGET)) { ExpenseTrackerScreen(services, onBack = { nav.popBackStack() }) }
                composable(AppRoutes.navRoute(AppRoutes.PROFILE_SETTINGS)) {
                    ProfileSettingsScreen(
                        services, onBack = { nav.popBackStack() }, navigate = navigateNamed(nav),
                        // signout(): pushNamedAndRemoveUntil('/login') — the root shows Login once signed out.
                        toLogin = { nav.popBackStack(AppRoutes.navRoute(AppRoutes.ROOT), inclusive = false) },
                    )
                }
                composable(AppRoutes.navRoute(AppRoutes.EDIT_PROFILE)) {
                    EditProfileScreen(services, onBack = { nav.popBackStack() })
                }
                composable(AppRoutes.navRoute(AppRoutes.COURSE_PAGE)) {
                    CourseListScreen(services, onBack = { nav.popBackStack() }) { folder ->
                        nav.navigate("learning/${Uri.encode(folder)}")
                    }
                }
                // MaterialPageRoute pushes in Flutter (no route names): internal routes.
                composable("learning/{subfolder}") { entry ->
                    val folder = entry.arguments?.getString("subfolder").orEmpty()
                    LearningScreen(folder, onBack = { nav.popBackStack() }) { index ->
                        nav.navigate("lesson/${Uri.encode(folder)}/$index")
                    }
                }
                composable("lesson/{subfolder}/{index}") { entry ->
                    LessonScreen(
                        services,
                        entry.arguments?.getString("subfolder").orEmpty(),
                        entry.arguments?.getString("index")?.toIntOrNull() ?: 0,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(AppRoutes.navRoute(AppRoutes.PRACTICE)) {
                    PracticeListScreen(onBack = { nav.popBackStack() }) { index -> nav.navigate("mcq/$index") }
                }
                composable("mcq/{index}") { entry ->
                    McqScreen(services, entry.arguments?.getString("index")?.toIntOrNull() ?: 0, onBack = { nav.popBackStack() })
                }
                composable(AppRoutes.navRoute(AppRoutes.BUDGET)) { ExpenseTrackerScreen(services, onBack = { nav.popBackStack() }) }
                AppRoutes.NAMED.filter { it !in MIGRATED }.forEach { name ->
                    composable(AppRoutes.navRoute(name)) {
                        NotMigratedScreen(name, onBack = { nav.popBackStack() })
                    }
                }
            }
          }
        }
    }
}

private val MIGRATED = setOf(
    AppRoutes.LOGIN, AppRoutes.SIGNUP, AppRoutes.ONBOARDING, AppRoutes.HOME, AppRoutes.PROFILE_SETTINGS,
    AppRoutes.BUDGET, AppRoutes.COURSE_PAGE, AppRoutes.PRACTICE, AppRoutes.EDIT_PROFILE,
)

/** `Navigator.pushNamed(context, name)` for a Flutter route name. */
fun navigateNamed(nav: NavHostController): (String) -> Unit = { name -> nav.navigate(AppRoutes.navRoute(name)) }

/**
 * Flutter pushes '/home' after sign-in; here Home is what the root
 * AuthWrapper renders once signed in, so "go home" pops back to the root.
 */
fun authNav(nav: NavHostController) = AuthNav(
    toHome = { nav.popBackStack(AppRoutes.navRoute(AppRoutes.ROOT), inclusive = false) },
    toOnboarding = { nav.navigate(AppRoutes.navRoute(AppRoutes.ONBOARDING)) },
    toSignUp = { nav.navigate(AppRoutes.navRoute(AppRoutes.SIGNUP)) },
    toLogin = { nav.navigate(AppRoutes.navRoute(AppRoutes.LOGIN)) },
)

/** main_functions.dart:11-23 — renders Home or Login inline from auth state. */
@Composable
fun AuthWrapper(services: PennyServices, nav: NavHostController) {
    val state by services.auth.state.collectAsState()
    when (AppRoutes.rootFor(state)) {
        AppRoutes.RootScreen.LOADING ->
            CircularProgressIndicator(Modifier.testTag("auth-loading"))
        AppRoutes.RootScreen.HOME -> HomeScreen(services, navigateNamed(nav))
        AppRoutes.RootScreen.LOGIN -> LoginScreen(services, authNav(nav))
    }
}

/** Placeholder for a Flutter route whose screen is not migrated yet. */
@Composable
fun NotMigratedScreen(route: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp).testTag("not-migrated"), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$route — not migrated yet", Modifier.testTag("not-migrated-route"))
            OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp).testTag("not-migrated-back")) {
                Text("Back")
            }
        }
    }
}
