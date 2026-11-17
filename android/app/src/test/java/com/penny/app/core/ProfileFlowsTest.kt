package com.penny.app.core

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Profile Settings / Edit Profile flows (profile_settings.dart, edit_profile.dart) on the fakes. */
class ProfileFlowsTest {

    private class World {
        val auth = FakeAuthService.demo()
        val deleted = mutableListOf<String>()
        val dir = runBlocking { FakeUserDirectory.demo(auth) { deleted += it } }
        val prefs = InMemoryUserPrefs()
        val images = FakeProfileImages()
        init { runBlocking { auth.login(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD) } }
    }

    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `change password checks mismatch first, then Firebase errors, then succeeds`() = runBlocking {
        val w = World()
        assertEquals(ProfileFlows.Outcome.Failed("New passwords do not match"),
            ProfileFlows.changePassword(w.auth, "penny123", "newpass1", "newpass2"))
        assertEquals(ProfileFlows.Outcome.Failed("Failed to change password: ${FakeAuthService.INVALID_CREDENTIAL}"),
            ProfileFlows.changePassword(w.auth, "wrong", "newpass1", "newpass1"))
        assertEquals(ProfileFlows.Outcome.Failed("Failed to change password: ${FakeAuthService.WEAK_PASSWORD}"),
            ProfileFlows.changePassword(w.auth, "penny123", "123", "123"))
        assertEquals(ProfileFlows.Outcome.Done("Password changed successfully"),
            ProfileFlows.changePassword(w.auth, "penny123", "newpass1", "newpass1"))
        w.auth.signOut()
        assertNull(w.auth.login(FakeAuthService.DEMO_EMAIL, "penny123"))
        assertEquals(FakeAuthService.DEMO_EMAIL, w.auth.login(FakeAuthService.DEMO_EMAIL, "newpass1")?.email)
    }

    @Test
    fun `change password without a session or an email reports Dart texts`() = runBlocking {
        val w = World()
        w.auth.signOut()
        assertEquals(ProfileFlows.Outcome.Failed("Failed to change password: Exception: No user currently signed in"),
            ProfileFlows.changePassword(w.auth, "a", "b", "b"))
        w.auth.verifyPhoneNumber("+1 555 0100")
        w.auth.signInWithSmsCode(FakeAuthService.FAKE_SMS_CODE)
        assertEquals(ProfileFlows.Outcome.Failed("Failed to change password: Null check operator used on a null value"),
            ProfileFlows.changePassword(w.auth, "a", "bbbbbb", "bbbbbb"))
    }

    @Test
    fun `delete account cascades and signs out`() = runBlocking {
        val w = World()
        w.prefs.saveUsername("demo")
        assertEquals(ProfileFlows.Outcome.Done("Account deleted successfully"),
            ProfileFlows.deleteAccount(w.auth, w.dir, w.prefs, "demo", profileImageUrl = null))
        assertNull(w.dir.getUser("demo"))
        assertEquals(listOf("demo"), w.deleted)
        assertEquals(AuthState.SignedOut, w.auth.state.value)
        assertNull(w.prefs.username())
        assertNull(w.auth.login(FakeAuthService.DEMO_EMAIL, FakeAuthService.DEMO_PASSWORD)) // auth account gone
    }

    @Test
    fun `delete account with a picture fails after the server data is gone`() = runBlocking {
        val w = World()
        assertEquals(
            ProfileFlows.Outcome.Failed("An error occurred while deleting account: ${ProfileFlows.STORAGE_OBJECT_NOT_FOUND}"),
            ProfileFlows.deleteAccount(w.auth, w.dir, w.prefs, "demo", profileImageUrl = "https://signed.example/p.jpg"),
        )
        assertNull(w.dir.getUser("demo")) // backend already deleted it
        assertEquals(AuthState.SignedIn::class, w.auth.state.value::class) // but the account survives
    }

    @Test
    fun `delete account guard and server failure texts`() = runBlocking {
        val w = World()
        assertEquals(ProfileFlows.Outcome.Failed("An error occurred while deleting account: Exception: Failed to delete account on server"),
            ProfileFlows.deleteAccount(w.auth, w.dir, w.prefs, "nobody", null))
        assertEquals(ProfileFlows.Outcome.Failed("An error occurred while deleting account: Exception: No user currently signed in"),
            ProfileFlows.deleteAccount(w.auth, w.dir, w.prefs, null, null))
    }

    @Test
    fun `edit fields prefill with empty fallbacks, and the username field shows a space`() {
        assertEquals(ProfileFlows.Fields("A", "B", "a@b.co", "1"),
            ProfileFlows.fields(obj("""{"user":{"firstName":"A","lastName":"B","email":"a@b.co","phone":"1","coins":0}}""")))
        assertEquals(ProfileFlows.Fields("A", "", "", ""), ProfileFlows.fields(obj("""{"user":{"firstName":"A","lastName":null}}""")))
        assertEquals(ProfileFlows.Fields("", "", "", ""), ProfileFlows.fields(obj("""{"user":{"firstName":"A","phone":5}}""")))
        assertEquals(ProfileFlows.Fields("", "", "", ""), ProfileFlows.fields(null))
        assertEquals(" ", ProfileFlows.usernameField(null))
    }

    @Test
    fun `save fires the four identical toasts, drops coins, updates claims, always saves the full name`() = runBlocking {
        val w = World()
        val r = ProfileFlows.saveProfile(w.auth, w.dir, w.prefs, "demo", ProfileFlows.Fields("Dee", "Mo", "d@m.io", "9"))
        assertEquals(ProfileFlows.SaveResult.Toasts(ProfileFlows.Toast.entries.map { it to "Profile Updated Successfully" }), r)
        assertEquals("""{"user":{"firstName":"Dee","lastName":"Mo","email":"d@m.io","phone":"9"}}""", w.dir.getUser("demo").toString())
        assertEquals(mapOf("username" to "demo", "fullName" to "Dee Mo"), w.auth.customClaims())
        assertEquals("Dee Mo", w.prefs.fullName())
    }

    @Test
    fun `save failures and missing username still save the full name`() = runBlocking {
        val w = World()
        assertEquals(ProfileFlows.SaveResult.SnackBar("An error occurred while updating profile: Exception: Failed to update profile"),
            ProfileFlows.saveProfile(w.auth, w.dir, w.prefs, "nobody", ProfileFlows.Fields("X", "Y", "", "")))
        assertEquals("X Y", w.prefs.fullName())
        assertEquals(ProfileFlows.SaveResult.Silent,
            ProfileFlows.saveProfile(w.auth, w.dir, w.prefs, null, ProfileFlows.Fields("P", "Q", "", "")))
        assertEquals("P Q", w.prefs.fullName())
    }

    @Test
    fun `picture update and delete, including deleting when there is none`() = runBlocking {
        val w = World()
        assertEquals(ProfileFlows.Outcome.Silent, ProfileFlows.updatePicture(w.images, "demo", picked = null))
        assertEquals(ProfileFlows.Outcome.Done("Profile picture updated successfully"),
            ProfileFlows.updatePicture(w.images, "demo", "content://picked/1"))
        assertEquals("content://picked/1", w.images.imageUrl("demo"))
        assertEquals(ProfileFlows.Outcome.Done("Profile picture deleted successfully"), ProfileFlows.deletePicture(w.images, "demo"))
        // "No profile picture found" is still a 200 → same success text.
        assertEquals(ProfileFlows.Outcome.Done("Profile picture deleted successfully"), ProfileFlows.deletePicture(w.images, "demo"))
        assertNull(w.images.imageUrl("demo"))
    }
}
