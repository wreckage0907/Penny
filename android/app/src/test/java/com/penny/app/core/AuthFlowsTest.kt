package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Decision logic of the auth pages (login.dart, signup.dart,
 * onboarding_page.dart) against the fakes that mimic Firebase + the
 * onboarding router.
 */
class AuthFlowsTest {

    private fun world(): Triple<FakeAuthService, FakeUserDirectory, InMemoryUserPrefs> {
        val auth = FakeAuthService.demo()
        val dir = runBlocking { FakeUserDirectory.demo(auth) }
        return Triple(auth, dir, InMemoryUserPrefs())
    }

    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `login by username resolves the email, signs in and saves prefs`() = runBlocking {
        val (auth, dir, prefs) = world()
        assertEquals(AuthNext.HOME, AuthFlows.login("demo", "penny123", dir, auth, prefs))
        assertEquals("demo", prefs.username())
        assertEquals("Demo User", prefs.fullName())
        assertEquals(AuthState.SignedIn::class, auth.state.value::class)
    }

    @Test
    fun `every login failure is silent and saves nothing`() = runBlocking {
        val (auth, dir, prefs) = world()
        assertEquals(AuthNext.NOTHING, AuthFlows.login("demo", "wrong", dir, auth, prefs))
        assertEquals(AuthNext.NOTHING, AuthFlows.login("nobody", "penny123", dir, auth, prefs))
        // Login takes a USERNAME: typing the email is a lookup miss.
        assertEquals(AuthNext.NOTHING, AuthFlows.login(FakeAuthService.DEMO_EMAIL, "penny123", dir, auth, prefs))
        assertEquals(AuthNext.NOTHING, AuthFlows.login("", "penny123", dir, auth, prefs))
        assertNull(prefs.username())
        assertEquals(AuthState.SignedOut, auth.state.value)
    }

    @Test
    fun `getUserData interpolates like Dart`() {
        assertEquals("a@b.co" to "Ann Lee", AuthFlows.emailAndFullName(
            obj("""{"user":{"email":"a@b.co","firstName":"Ann","lastName":"Lee"}}""")))
        // '${null} ${...}' prints "null".
        assertEquals("a@b.co" to "null Lee", AuthFlows.emailAndFullName(
            obj("""{"user":{"email":"a@b.co","lastName":"Lee"}}""")))
        assertEquals(null to "Ann Lee", AuthFlows.emailAndFullName(
            obj("""{"user":{"email":null,"firstName":"Ann","lastName":"Lee"}}""")))
        // data['user'] == null → both null; non-string email → TypeError → both null.
        assertEquals(null to null, AuthFlows.emailAndFullName(obj("""{"other":1}""")))
        assertEquals(null to null, AuthFlows.emailAndFullName(obj("""{"user":{"email":5}}""")))
        assertEquals(null to null, AuthFlows.emailAndFullName(null))
    }

    @Test
    fun `sign-up goes to onboarding, failures stay put`() = runBlocking {
        val (auth, _, _) = world()
        assertEquals(AuthNext.NOTHING, AuthFlows.signUp("x@y.io", "12345", auth))
        assertEquals(AuthNext.ONBOARDING, AuthFlows.signUp("x@y.io", "123456", auth))
    }

    @Test
    fun `onboarding posts the profile, sets claims and goes home`() = runBlocking {
        val (auth, dir, _) = world()
        AuthFlows.signUp("x@y.io", "123456", auth)
        val r = AuthFlows.submitOnboarding(NewUser("xy", "Xavier", "Young", "x@y.io", "+1"), dir, auth)
        assertEquals(AuthFlows.OnboardingResult.Home, r)
        assertEquals(mapOf("username" to "xy", "fullName" to "Xavier Young"), auth.customClaims())
        assertEquals("x@y.io" to "Xavier Young", AuthFlows.emailAndFullName(dir.getUser("xy")))
    }

    @Test
    fun `onboarding error texts are Dart's`() = runBlocking {
        val (auth, dir, _) = world()
        AuthFlows.signUp("x@y.io", "123456", auth)
        assertEquals(
            AuthFlows.OnboardingResult.Error(
                "An error occurred: Exception: Failed to post user data: {\"detail\":\"Not Found\"}",
            ),
            AuthFlows.submitOnboarding(NewUser("", "a", "b", "c", "d"), dir, auth),
        )
        auth.signOut()
        assertEquals(
            AuthFlows.OnboardingResult.Error("An error occurred: Exception: No user is currently signed in"),
            AuthFlows.submitOnboarding(NewUser("z", "a", "b", "c", "d"), dir, auth),
        )
    }

    @Test
    fun `onboarding overwrites an existing username like the backend`() = runBlocking {
        val (auth, dir, _) = world()
        AuthFlows.signUp("x@y.io", "123456", auth)
        AuthFlows.submitOnboarding(NewUser("demo", "Mallory", "M", "x@y.io", "1"), dir, auth)
        assertEquals("x@y.io" to "Mallory M", AuthFlows.emailAndFullName(dir.getUser("demo")))
    }

    @Test
    fun `google onboards only new users, github on login also without username`() = runBlocking {
        val (auth, _, _) = world()
        val g1 = auth.signInWithProvider(SocialProvider.GOOGLE)
        assertEquals(AuthNext.ONBOARDING, AuthFlows.afterGoogle(g1))
        assertEquals(AuthNext.HOME, AuthFlows.afterGoogle(auth.signInWithProvider(SocialProvider.GOOGLE)))

        val gh1 = auth.signInWithProvider(SocialProvider.GITHUB)
        assertEquals(AuthNext.ONBOARDING, AuthFlows.afterGithub(gh1, onLoginPage = false))
        val gh2 = auth.signInWithProvider(SocialProvider.GITHUB) // not new, but never onboarded
        assertEquals(AuthNext.ONBOARDING, AuthFlows.afterGithub(gh2, onLoginPage = true))
        assertEquals(AuthNext.HOME, AuthFlows.afterGithub(gh2, onLoginPage = false))
        assertEquals(AuthNext.NOTHING, AuthFlows.afterGoogle(SocialSignIn(null, error = "x")))
    }

    @Test
    fun `phone flow needs a code-sent number and the fake code`() = runBlocking {
        val (auth, _, _) = world()
        assertEquals(false, auth.signInWithSmsCode("123456")) // no code sent yet
        assertEquals(false, auth.verifyPhoneNumber("5550100")) // not E.164
        assertEquals(true, auth.verifyPhoneNumber("+1 555 0100"))
        assertEquals(false, auth.signInWithSmsCode("000000"))
        assertEquals(AuthState.SignedOut, auth.state.value)
        assertEquals(true, auth.signInWithSmsCode(FakeAuthService.FAKE_SMS_CODE))
        assertEquals(AuthState.SignedIn::class, auth.state.value::class)
    }

    @Test
    fun `claims are null signed out and empty before onboarding`() = runBlocking {
        val (auth, _, _) = world()
        assertNull(auth.customClaims())
        auth.login(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD)
        assertEquals(mapOf("username" to "demo", "fullName" to "Demo User"), auth.customClaims())
    }
}
