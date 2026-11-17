package com.penny.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

/** Route table (main.dart:44-56) and AuthWrapper decision (main_functions.dart:11-23). */
class AppRoutesTest {

    @Test
    fun `named routes are main dart's map verbatim, in order`() {
        assertEquals(
            listOf(
                "/login", "/signup", "/home", "/coursepage", "/chatbot", "/budget", "/practice",
                "/onboarding", "/stockprofile", "/profilesettings", "/editprofile",
            ),
            AppRoutes.NAMED,
        )
    }

    @Test
    fun `auth wrapper shows spinner while waiting, home with a user, login otherwise`() {
        assertEquals(AppRoutes.RootScreen.LOADING, AppRoutes.rootFor(AuthState.Loading))
        assertEquals(AppRoutes.RootScreen.LOGIN, AppRoutes.rootFor(AuthState.SignedOut))
        assertEquals(
            AppRoutes.RootScreen.HOME,
            AppRoutes.rootFor(AuthState.SignedIn(PennyUser("u", "e@x.io"))),
        )
    }

    @Test
    fun `nav routes drop the leading slash and root has a name`() {
        assertEquals("root", AppRoutes.navRoute("/"))
        assertEquals("coursepage", AppRoutes.navRoute("/coursepage"))
        assertEquals(AppRoutes.NAMED.size, AppRoutes.NAMED.map(AppRoutes::navRoute).toSet().size)
    }

    @Test
    fun `colours are app_colours dart`() {
        assertEquals(0xFF35333A, PennyColors.TEXT)
        assertEquals(0xFFF9F5F2, PennyColors.BACKGROUND)
        assertEquals(0xFFE4C7C3, PennyColors.CARD)
        assertEquals(0xFFA35555, PennyColors.BUTTON)
    }
}
