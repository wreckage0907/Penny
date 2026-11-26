package com.penny.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.penny.app.PennyServices
import com.penny.app.core.AppRoutes

/**
 * v2.1 navigation (artboard routes): the four-tab shell — Home / Budget /
 * Learn / Profile — plus Penny AI, Account Recovery, the learning journey's
 * lesson/quiz/results stack, and the retained stock practice and
 * social/OTP onboarding screens.
 *
 * Destinations are nav routes without the leading slash; strings that start
 * with "/" are Flutter-era names mapped through [AppRoutes.navRoute] so
 * old call sites keep working.
 */
@Composable
fun PennyApp(services: PennyServices, nav: NavHostController = rememberNavController()) {
    PennyTheme {
        // targetSdk 36 is edge-to-edge: keep content clear of the system bars.
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                NavHost(nav, startDestination = AppRoutes.navRoute(AppRoutes.ROOT)) {
                    composable(AppRoutes.navRoute(AppRoutes.ROOT)) { AuthWrapper(services, nav) }
                    composable("login") { LoginScreen(services, authNav(nav)) }
                    composable("login/recover") { RecoveryScreen(services, authNav(nav)) }
                    composable("signup") { SignUpScreen(services, authNav(nav)) }
                    composable("onboarding") { OnboardingScreen(services, authNav(nav)) }
                    composable("home") { HomeScreen(services, v21Navigate(nav)) }
                    composable(
                        route = "budget?add={add}&tab={tab}&income={income}",
                        arguments = listOf(
                            navArgument("add") { type = NavType.StringType; defaultValue = "" },
                            navArgument("tab") { type = NavType.StringType; defaultValue = "" },
                            navArgument("income") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) { entry ->
                        BudgetScreen(
                            services,
                            v21Navigate(nav),
                            initialTab = entry.arguments?.getString("tab")?.takeIf { it.isNotEmpty() },
                            openAdd = entry.arguments?.getString("add") == "true",
                            openIncome = entry.arguments?.getString("income") == "true",
                        )
                    }
                    composable(
                        route = "learn?unit={unit}",
                        arguments = listOf(
                            navArgument("unit") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) { entry ->
                        LearnScreen(
                            services,
                            v21Navigate(nav),
                            initialUnit = entry.arguments?.getString("unit")?.takeIf { it.isNotEmpty() },
                        )
                    }
                    composable("lesson/{unit}/{index}") { entry ->
                        LessonScreen(
                            services,
                            entry.arguments?.getString("unit").orEmpty(),
                            entry.arguments?.getString("index")?.toIntOrNull() ?: 0,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        route = "quiz/{unit}?review={review}",
                        arguments = listOf(
                            navArgument("review") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) { entry ->
                        QuizScreen(
                            services,
                            entry.arguments?.getString("unit").orEmpty(),
                            review = entry.arguments?.getString("review") == "true",
                            onBack = { nav.popBackStack() },
                            navigate = v21Navigate(nav),
                        )
                    }
                    composable("results/{unit}") { entry ->
                        ResultsScreen(
                            services,
                            entry.arguments?.getString("unit").orEmpty(),
                            onBack = { nav.popBackStack() },
                            navigate = v21Navigate(nav),
                        )
                    }
                    composable("ai") { PennyAiScreen(services, v21Navigate(nav)) }
                    composable(
                        route = "profilesettings?saved={saved}",
                        arguments = listOf(
                            navArgument("saved") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) { entry ->
                        ProfileScreen(
                            services,
                            navigate = v21Navigate(nav),
                            // signout: pushNamedAndRemoveUntil('/login') — the root shows Login once signed out.
                            toLogin = { nav.popBackStack(AppRoutes.navRoute(AppRoutes.ROOT), inclusive = false) },
                            savedBanner = entry.arguments?.getString("saved") == "true",
                        )
                    }
                    composable("editprofile") {
                        EditProfileScreen(
                            services,
                            onBack = { nav.popBackStack() },
                            saved = {
                                nav.popBackStack()
                                nav.navigate("profilesettings?saved=true")
                            },
                        )
                    }
                    composable("stockprofile") { StockScreen(services, onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}

/** v2.1 navigate: Flutter-era "/name" strings map; new routes pass verbatim. */
fun v21Navigate(nav: NavHostController): (String) -> Unit = { dest ->
    if (dest.startsWith("/")) nav.navigate(AppRoutes.navRoute(dest)) else nav.navigate(dest)
}

/** `Navigator.pushNamed(context, name)` for a Flutter route name. */
fun navigateNamed(nav: NavHostController): (String) -> Unit = { name -> nav.navigate(AppRoutes.navRoute(name)) }

/**
 * The v2.1 auth graph: Sign In → root (Home when signed in), Create Account
 * → root, social/OTP completion → Onboarding → root.
 */
fun authNav(nav: NavHostController) = AuthNav(
    toHome = { nav.popBackStack(AppRoutes.navRoute(AppRoutes.ROOT), inclusive = false) },
    toOnboarding = { nav.navigate("onboarding") },
    toSignUp = { nav.navigate("signup") },
    toLogin = { nav.navigate("login") },
    toRecovery = { nav.navigate("login/recover") },
)

/** The root decides Login vs Home from auth state (main_functions.dart:11-23). */
@Composable
fun AuthWrapper(services: PennyServices, nav: NavHostController) {
    val state by services.auth.state.collectAsState()
    when (AppRoutes.rootFor(state)) {
        AppRoutes.RootScreen.LOADING ->
            CircularProgressIndicator(Modifier.testTag("auth-loading"))
        AppRoutes.RootScreen.HOME -> HomeScreen(services, v21Navigate(nav))
        AppRoutes.RootScreen.LOGIN -> LoginScreen(services, authNav(nav))
    }
}
