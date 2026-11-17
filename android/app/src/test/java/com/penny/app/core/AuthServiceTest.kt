package com.penny.app.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The fake auth mimics the Firebase rules auth.dart branches on (null on any
 * failure, sign-up signs in). It is NEW behaviour by design (maintainer:
 * mimic services until the stack is chosen), so these tests pin its contract.
 */
class AuthServiceTest {

    @Test
    fun `starts signed out unless seeded as signed in`() {
        assertEquals(AuthState.SignedOut, FakeAuthService.demo().state.value)
        val signedIn = FakeAuthService(mapOf("a@b.co" to "secret1"), signedInAs = "A@B.CO")
        assertEquals("a@b.co", (signedIn.state.value as AuthState.SignedIn).user.email)
    }

    @Test
    fun `demo account logs in and signs out`() = runBlocking {
        val auth = FakeAuthService.demo()
        val user = auth.login(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD)
        assertNotNull(user)
        assertEquals(AuthState.SignedIn(user!!), auth.state.value)
        auth.signOut()
        assertEquals(AuthState.SignedOut, auth.state.value)
    }

    @Test
    fun `failures return null and leave state unchanged`() = runBlocking {
        val auth = FakeAuthService.demo()
        assertNull(auth.login(FakeAuthService.DEMO_EMAIL, "wrong-password"))
        assertNull(auth.login("nobody@penny.app", "penny123"))
        assertNull(auth.login("not-an-email", "penny123"))
        assertNull(auth.signUp("new@penny.app", "12345")) // weak-password: < 6
        assertNull(auth.signUp("DEMO@penny.app", "longenough")) // email-already-in-use, case-insensitive
        assertNull(auth.signUp("bad email@penny.app", "longenough")) // invalid-email
        assertEquals(AuthState.SignedOut, auth.state.value)
    }

    @Test
    fun `sign-up creates the account and signs it in, then it can log in again`() = runBlocking {
        val auth = FakeAuthService.demo()
        val user = auth.signUp("new@penny.app", "123456")!!
        assertEquals(AuthState.SignedIn(user), auth.state.value)
        auth.signOut()
        assertEquals(user, auth.login("NEW@penny.app", "123456"))
        assertEquals("fake-uid-2", user.uid)
    }
}
