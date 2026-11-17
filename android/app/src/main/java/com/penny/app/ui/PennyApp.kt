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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.penny.app.PennyServices
import com.penny.app.core.AppRoutes
import com.penny.app.core.FakeAuthService
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
                AppRoutes.NAMED.forEach { name ->
                    composable(AppRoutes.navRoute(name)) {
                        NotMigratedScreen(name, onBack = { nav.popBackStack() })
                    }
                }
            }
          }
        }
    }
}

/** main_functions.dart:11-23 — renders Home or Login inline from auth state. */
@Composable
fun AuthWrapper(services: PennyServices, nav: NavHostController) {
    val state by services.auth.state.collectAsState()
    when (AppRoutes.rootFor(state)) {
        AppRoutes.RootScreen.LOADING ->
            CircularProgressIndicator(Modifier.testTag("auth-loading"))
        AppRoutes.RootScreen.HOME -> ShellHome(services, nav)
        AppRoutes.RootScreen.LOGIN -> ShellLogin(services)
    }
}

/** TEMPORARY (006): stands in for LoginPage until slice 007. */
@Composable
private fun ShellLogin(services: PennyServices) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().padding(24.dp).testTag("shell-login"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Penny — login not migrated yet", Modifier.testTag("shell-login-title"))
        Button(
            onClick = {
                scope.launch {
                    services.auth.login(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD)
                }
            },
            modifier = Modifier.padding(top = 16.dp).testTag("shell-demo-login"),
        ) { Text("Continue with demo account") }
    }
}

/** TEMPORARY (006): stands in for Home until slice 008 — lists every route. */
@Composable
private fun ShellHome(services: PennyServices, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
            .testTag("shell-home"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Penny — home not migrated yet", Modifier.testTag("shell-home-title"))
        AppRoutes.NAMED.filter { it != AppRoutes.LOGIN && it != AppRoutes.HOME }.forEach { name ->
            OutlinedButton(
                onClick = { nav.navigate(AppRoutes.navRoute(name)) },
                modifier = Modifier.testTag("shell-route-$name"),
            ) { Text(name) }
        }
        Button(
            onClick = { scope.launch { services.auth.signOut() } },
            modifier = Modifier.testTag("shell-sign-out"),
        ) { Text("Sign out") }
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
