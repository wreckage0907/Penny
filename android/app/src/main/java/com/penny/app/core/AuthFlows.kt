package com.penny.app.core

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Backend user directory: `GET/POST /onboarding/{username}` and
 * `POST /set-custom-claims` (backend/routers/onboarding.py). Payloads stay
 * raw JSON (`{"user": {...}}`) like the 002 client, so screens read them with
 * the same Dart-shaped logic whatever implements this.
 */
interface UserDirectory {
    /** 200 → the stored document; 404 → null. */
    suspend fun getUser(username: String): JsonObject?

    /** POST; failures throw with the Dart message the screen renders. */
    suspend fun createUser(username: String, profile: NewUser): JsonObject

    suspend fun setCustomClaims(uid: String, username: String, fullName: String)
}

/** NewUser (onboarding_page.dart:15-29). */
data class NewUser(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNo: String,
)

/** Thrown by [UserDirectory.createUser]; `message` is Dart's `$e` text. */
class OnboardingException(message: String) : RuntimeException(message)

/**
 * In-memory mimic of the onboarding router: POST overwrites any existing
 * username (no uniqueness check, coins reset to 0), GET 404s unknown names,
 * custom claims land on the fake auth account. An empty or slash-containing
 * username never reaches the handler (FastAPI 404 "Not Found").
 */
class FakeUserDirectory(private val auth: FakeAuthService) : UserDirectory {
    private val docs = linkedMapOf<String, JsonObject>()

    override suspend fun getUser(username: String): JsonObject? =
        if (username.isEmpty() || '/' in username) null else docs[username]

    override suspend fun createUser(username: String, profile: NewUser): JsonObject {
        if (username.isEmpty() || '/' in username) {
            throw OnboardingException("Exception: Failed to post user data: {\"detail\":\"Not Found\"}")
        }
        val doc = buildJsonObject {
            put(
                "user",
                buildJsonObject {
                    put("firstName", profile.firstName)
                    put("lastName", profile.lastName)
                    put("email", profile.email)
                    put("phone", profile.phoneNo)
                    put("coins", 0)
                },
            )
        }
        docs[username] = doc
        return doc
    }

    override suspend fun setCustomClaims(uid: String, username: String, fullName: String) {
        auth.setCustomClaims(uid, mapOf("username" to username, "fullName" to fullName))
    }

    companion object {
        /** Directory with the demo account onboarded (username "demo"). */
        suspend fun demo(auth: FakeAuthService): FakeUserDirectory {
            val dir = FakeUserDirectory(auth)
            dir.createUser("demo", NewUser("demo", "Demo", "User", FakeAuthService.DEMO_EMAIL, "+1 555 0100"))
            val uid = auth.uidOf(FakeAuthService.DEMO_EMAIL)
            if (uid != null) dir.setCustomClaims(uid, "demo", "Demo User")
            return dir
        }
    }
}

/** SharedPreferences keys auth.dart uses (`username`, `fullName`). */
interface UserPrefs {
    suspend fun saveUsername(username: String)
    suspend fun saveFullName(fullName: String)
    suspend fun username(): String?
    suspend fun fullName(): String?
    suspend fun clear()
}

/** Process-local prefs (the fake session is process-local too). */
class InMemoryUserPrefs : UserPrefs {
    private var username: String? = null
    private var fullName: String? = null
    override suspend fun saveUsername(username: String) { this.username = username }
    override suspend fun saveFullName(fullName: String) { this.fullName = fullName }
    override suspend fun username(): String? = username
    override suspend fun fullName(): String? = fullName
    override suspend fun clear() { username = null; fullName = null }
}

/** Where a flow sends the user next; NOTHING = Flutter only `print`s. */
enum class AuthNext { HOME, ONBOARDING, NOTHING }

/**
 * The decision logic of the auth pages, independent of Compose:
 * login.dart:32-60,185-203; signup.dart:151-158; onboarding_page.dart:268-305;
 * the social branches login.dart:113-157 / signup.dart:80-123.
 */
object AuthFlows {

    /**
     * getUserData (login.dart:32-60): `{'email': user.email, 'fullName':
     * '${firstName} ${lastName}'}` — Dart interpolates null as "null".
     * A missing `user` key, a 404, or a non-string email yields no email.
     */
    fun emailAndFullName(doc: JsonObject?): Pair<String?, String?> {
        val user = doc?.get("user") as? JsonObject ?: return null to null
        val email = user["email"]
        if (email != null && !(email is JsonPrimitive && (email.isString || email.content == "null"))) {
            return null to null // Dart: TypeError building Map<String, String?> → caught → nulls
        }
        fun part(key: String): String = (user[key] as? JsonPrimitive)?.content ?: "null"
        val emailText = (email as? JsonPrimitive)?.takeIf { it.isString }?.content
        return emailText to "${part("firstName")} ${part("lastName")}"
    }

    /**
     * "Sign In" (login.dart:185-203): username → directory email → auth
     * login; on success save username + fullName and go home. Every failure
     * (unknown username, wrong password, network) is silent in Flutter.
     */
    suspend fun login(
        username: String,
        password: String,
        directory: UserDirectory,
        auth: AuthService,
        prefs: UserPrefs,
    ): AuthNext {
        val doc = try {
            directory.getUser(username)
        } catch (e: Exception) {
            null
        }
        val (email, fullName) = emailAndFullName(doc)
        if (email == null) return AuthNext.NOTHING
        auth.login(email, password) ?: return AuthNext.NOTHING
        prefs.saveUsername(username)
        if (fullName != null) prefs.saveFullName(fullName)
        return AuthNext.HOME
    }

    /** "Sign Up" (signup.dart:151-158): success → onboarding, else silent. */
    suspend fun signUp(email: String, password: String, auth: AuthService): AuthNext =
        if (auth.signUp(email, password) != null) AuthNext.ONBOARDING else AuthNext.NOTHING

    /** Google button: onboarding only for a new user (login.dart:115-120). */
    fun afterGoogle(result: SocialSignIn): AuthNext = when {
        result.user == null -> AuthNext.NOTHING
        result.isNewUser -> AuthNext.ONBOARDING
        else -> AuthNext.HOME
    }

    /**
     * GitHub button: login.dart:148 also onboards when the username claim is
     * missing; signup.dart:115 checks `isNewUser` only — both kept.
     */
    fun afterGithub(result: SocialSignIn, onLoginPage: Boolean): AuthNext = when {
        result.user == null -> AuthNext.NOTHING
        result.isNewUser || (onLoginPage && result.username == null) -> AuthNext.ONBOARDING
        else -> AuthNext.HOME
    }

    /**
     * signout (auth.dart:231-236): auth sign-out + clearUserData (removes the
     * `username` / `fullName` prefs); the caller then resets to login.
     */
    suspend fun signOut(auth: AuthService, prefs: UserPrefs) {
        auth.signOut()
        prefs.clear()
    }

    sealed interface OnboardingResult {
        data object Home : OnboardingResult
        data class Error(val snackbar: String) : OnboardingResult
    }

    /**
     * Submit (onboarding_page.dart:268-305): post the profile, then set the
     * custom claims for the signed-in user (fullName = "first last"), then
     * home. Any failure → SnackBar "An error occurred: $e".
     * The profile image upload (POST /prof) is not mimicked yet.
     */
    suspend fun submitOnboarding(
        user: NewUser,
        directory: UserDirectory,
        auth: AuthService,
    ): OnboardingResult = try {
        directory.createUser(user.username, user)
        val current = (auth.state.value as? AuthState.SignedIn)?.user
            ?: throw OnboardingException("Exception: No user is currently signed in")
        directory.setCustomClaims(current.uid, user.username, "${user.firstName} ${user.lastName}")
        OnboardingResult.Home
    } catch (e: OnboardingException) {
        OnboardingResult.Error("An error occurred: ${e.message}")
    } catch (e: Exception) {
        // Dart `catch (e)` catches everything (review 007): a real
        // implementation's transport error must still become the SnackBar.
        OnboardingResult.Error("An error occurred: $e")
    }
}
