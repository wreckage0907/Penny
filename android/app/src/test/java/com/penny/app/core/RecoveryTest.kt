package com.penny.app.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v2.1 auth additions (frames 01–03, S02): the complete account-recovery
 * flow on the fake (deterministic clock), reauthentication, email-or-
 * username login resolution, and sign-up-with-profile.
 */
class RecoveryTest {

    private fun service(signedInAs: String? = null, tick: () -> Long = { 0L }): FakeAuthService =
        FakeAuthService(
            mapOf(FakeAuthService.DEMO_EMAIL to FakeAuthService.DEMO_PASSWORD),
            signedInAs = signedInAs,
            nowMs = tick,
        )

    @Test
    fun `requesting a code only works for known accounts`() = runBlocking {
        val auth = service()
        assertTrue(auth.requestPasswordReset(FakeAuthService.DEMO_EMAIL))
        assertFalse(auth.requestPasswordReset("nobody@penny.app"))
        assertFalse(auth.requestPasswordReset("not-an-email"))
    }

    @Test
    fun `an unknown code is invalid and an expired one says so`() = runBlocking {
        var now = 0L
        val auth = service(tick = { now })
        auth.requestPasswordReset(FakeAuthService.DEMO_EMAIL)
        assertEquals(ResetResult.InvalidCode, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, "000000", "newpassword"))
        now = FakeAuthService.RESET_TTL_MS // exactly at the TTL the code is dead
        assertEquals(ResetResult.ExpiredCode, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.RESET_CODE, "newpassword"))
        // A dead code keeps answering "expired" — only a successful reset consumes the list.
        assertEquals(ResetResult.ExpiredCode, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.RESET_CODE, "newpassword"))
    }

    @Test
    fun `a successful reset changes the password and signs out existing sessions`() = runBlocking {
        val auth = service(signedInAs = FakeAuthService.DEMO_EMAIL)
        assertEquals(AuthState.SignedIn::class, auth.state.value::class)
        auth.requestPasswordReset(FakeAuthService.DEMO_EMAIL)
        assertEquals(ResetResult.WeakPassword, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.RESET_CODE, "abc"))
        assertEquals(ResetResult.Success, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.RESET_CODE, "newpassword"))
        assertEquals(AuthState.SignedOut, auth.state.value) // "Existing sessions will be signed out"
        assertNull(auth.login(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD)) // old password dead
        assertTrue(auth.login(FakeAuthService.DEMO_EMAIL, "newpassword") != null) // new one works
    }

    @Test
    fun `the demo seed carries an already-expired code - frame 02's state`() = runBlocking {
        val auth = service()
        auth.seedExpiredReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.EXPIRED_DEMO_CODE)
        assertEquals(ResetResult.ExpiredCode, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.EXPIRED_DEMO_CODE, "newpassword"))
        // A fresh request supersedes the seed; the old digits still read expired,
        // the new code works.
        auth.requestPasswordReset(FakeAuthService.DEMO_EMAIL)
        assertEquals(ResetResult.ExpiredCode, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.EXPIRED_DEMO_CODE, "newpassword"))
        assertEquals(ResetResult.Success, auth.completePasswordReset(FakeAuthService.DEMO_EMAIL, FakeAuthService.RESET_CODE, "newpassword"))
    }

    @Test
    fun `reauthentication checks the stored password`() = runBlocking {
        val auth = service()
        assertTrue(auth.reauthenticate(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD))
        assertFalse(auth.reauthenticate(FakeAuthService.DEMO_EMAIL, "wrong"))
        assertFalse(auth.reauthenticate("", FakeAuthService.DEMO_PASSWORD)) // phone accounts have none
    }
}

/** Frame 03 (sign-up folds onboarding) + frame 01 ("Email or username"). */
class AuthFlowsV21Test {

    private suspend fun world(seedDemo: Boolean = false): Triple<FakeAuthService, FakeUserDirectory, InMemoryUserPrefs> {
        val auth = FakeAuthService(mapOf("seed@penny.app" to "secret1"))
        val dir = FakeUserDirectory(auth)
        if (seedDemo) dir.createUser("demo", NewUser("demo", "Demo", "User", "demo@penny.app", "+1 555 0100"))
        return Triple(auth, dir, InMemoryUserPrefs())
    }

    @Test
    fun `sign-up with profile creates the doc, the claims, and lands home`() = runBlocking {
        val (auth, dir, prefs) = world()
        val result = AuthFlows.signUpWithProfile("New Bie", "newbie", "new@bie.io", "secret1", dir, auth)
        assertEquals(AuthFlows.SignUpResult.Home, result)
        val doc = dir.getUser("newbie")
        assertTrue(doc.toString().contains("\"firstName\":\"New\""))
        assertTrue(doc.toString().contains("\"lastName\":\"Bie\""))
        assertEquals(mapOf("username" to "newbie", "fullName" to "New Bie"), auth.customClaims())
    }

    @Test
    fun `a taken username never strands an auth account`() = runBlocking {
        val (auth, dir, prefs) = world(seedDemo = true)
        assertEquals(AuthFlows.SignUpResult.UsernameTaken, AuthFlows.signUpWithProfile("Demo Two", "demo", "x@y.io", "secret1", dir, auth))
        // No auth account was created for the email.
        assertNull(auth.login("x@y.io", "secret1"))
        assertEquals(AuthFlows.SignUpResult.InvalidUsername, AuthFlows.signUpWithProfile("N", "", "x@y.io", "secret1", dir, auth))
        assertEquals(AuthFlows.SignUpResult.InvalidUsername, AuthFlows.signUpWithProfile("N", "a/b", "x@y.io", "secret1", dir, auth))
    }

    @Test
    fun `a failed auth sign-up reports failure`() = runBlocking {
        val (auth, dir, prefs) = world()
        assertEquals(AuthFlows.SignUpResult.Failed, AuthFlows.signUpWithProfile("A B", "ab", "seed@penny.app", "secret1", dir, auth)) // email in use
        assertEquals(AuthFlows.SignUpResult.Failed, AuthFlows.signUpWithProfile("A B", "cd", "c@d.io", "12345", dir, auth)) // weak password
    }

    @Test
    fun `login identifiers resolve usernames and emails`() = runBlocking {
        val (auth, dir, prefs) = world(seedDemo = true)
        assertEquals("demo", AuthFlows.resolveLoginIdentifier("demo", dir))
        assertEquals("demo", AuthFlows.resolveLoginIdentifier("DEMO@PENNY.APP", dir)) // case-insensitive email
        assertNull(AuthFlows.resolveLoginIdentifier("who@penny.app", dir))
        assertNull(AuthFlows.resolveLoginIdentifier("", dir))
        assertNull(AuthFlows.resolveLoginIdentifier("  ", dir))
    }

    @Test
    fun `the directory answers email → username for existing docs only`() = runBlocking {
        val (auth, dir, prefs) = world(seedDemo = true)
        assertEquals("demo", dir.usernameForEmail("demo@penny.app"))
        assertNull(dir.usernameForEmail("missing@penny.app"))
    }
}
