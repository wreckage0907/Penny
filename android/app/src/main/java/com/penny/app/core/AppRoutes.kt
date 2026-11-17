package com.penny.app.core

/**
 * The Flutter route table (main.dart:44-56) and AuthWrapper's decision
 * (main_functions.dart:11-23). Route names are kept verbatim so screens can
 * `pushNamed` the same strings the Flutter pages use.
 */
object AppRoutes {
    const val LOGIN = "/login"
    const val SIGNUP = "/signup"
    const val HOME = "/home"
    const val COURSE_PAGE = "/coursepage"
    const val CHATBOT = "/chatbot"
    const val BUDGET = "/budget"
    const val PRACTICE = "/practice"
    const val ONBOARDING = "/onboarding"
    const val STOCK_PROFILE = "/stockprofile"
    const val PROFILE_SETTINGS = "/profilesettings"
    const val EDIT_PROFILE = "/editprofile"

    /** `home: AuthWrapper()` — the root that decides login vs home. */
    const val ROOT = "/"

    /** Declaration order of main.dart's `routes:` map. */
    val NAMED: List<String> = listOf(
        LOGIN, SIGNUP, HOME, COURSE_PAGE, CHATBOT, BUDGET, PRACTICE, ONBOARDING,
        STOCK_PROFILE, PROFILE_SETTINGS, EDIT_PROFILE,
    )

    /** Navigation-compose route for a Flutter route name (no leading slash). */
    fun navRoute(name: String): String = if (name == ROOT) "root" else name.removePrefix("/")

    enum class RootScreen { LOADING, HOME, LOGIN }

    /** waiting → spinner; user → Home; otherwise → LoginPage. */
    fun rootFor(state: AuthState): RootScreen = when (state) {
        AuthState.Loading -> RootScreen.LOADING
        is AuthState.SignedIn -> RootScreen.HOME
        AuthState.SignedOut -> RootScreen.LOGIN
    }
}

/** Colour constants (consts/app_colours.dart), ARGB. */
object PennyColors {
    const val TEXT = 0xFF35333A // rgb(53, 51, 58)
    const val BACKGROUND = 0xFFF9F5F2 // rgb(249, 245, 242)
    const val CARD = 0xFFE4C7C3 // rgb(228, 199, 195)
    const val BUTTON = 0xFFA35555 // rgb(163, 85, 85)
}
