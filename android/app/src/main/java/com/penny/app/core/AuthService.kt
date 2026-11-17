package com.penny.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A signed-in account (Firebase `User` stand-in: only what the app reads). */
data class PennyUser(val uid: String, val email: String)

/** `authStateChanges()` as observed by AuthWrapper (main_functions.dart:11-23). */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: PennyUser) : AuthState
}

/**
 * The auth seam. The Flutter app talks to Firebase Auth directly
 * (Services/auth.dart); the backend/auth stack is undecided (maintainer,
 * 2026-11-17), so screens depend on this interface and the app ships with
 * [FakeAuthService]. Swapping in Firebase or anything else later is an
 * implementation change only.
 *
 * Contract mirrored from auth.dart:15-51: `signUp` / `login` return null on
 * any auth failure (Flutter swallows FirebaseAuthException and returns null),
 * and a successful sign-up also signs the user in (Firebase behaviour).
 */
interface AuthService {
    val state: StateFlow<AuthState>
    suspend fun signUp(email: String, password: String): PennyUser?
    suspend fun login(email: String, password: String): PennyUser?
    suspend fun signOut()

    /** getCustomClaims (auth.dart:254-262): null when nobody is signed in. */
    suspend fun customClaims(): Map<String, String>?

    /** signinWithGoogle / signInWithGithub (auth.dart:137-229). */
    suspend fun signInWithProvider(provider: SocialProvider): SocialSignIn

    /** verifyPhoneNumber (input_field.dart:116-143): true = "codeSent". */
    suspend fun verifyPhoneNumber(phoneNumber: String): Boolean

    /** signInWithSmsCode (:145-157): true when signed in. */
    suspend fun signInWithSmsCode(smsCode: String): Boolean
}

enum class SocialProvider { GOOGLE, GITHUB }

/** The Map auth.dart returns, typed: `user` null ⇔ `error` set. */
data class SocialSignIn(
    val user: PennyUser?,
    val isNewUser: Boolean = false,
    val username: String? = null,
    val fullName: String? = null,
    val error: String? = null,
)

/**
 * In-memory stand-in mimicking Firebase email/password rules that the
 * Flutter code branches on: `weak-password` (< 6 chars),
 * `email-already-in-use` (emails are case-insensitive), `invalid-email`, and
 * wrong credentials — each surfaces as a null result, like auth.dart.
 * NEW behaviour, not parity: state is process-local (Firebase persists the
 * session across restarts), and no network or delays are simulated.
 */
class FakeAuthService(
    seedAccounts: Map<String, String> = emptyMap(),
    signedInAs: String? = null,
) : AuthService {
    private val passwords = linkedMapOf<String, String>()
    private val users = linkedMapOf<String, PennyUser>()

    init {
        seedAccounts.forEach { (email, password) -> create(email, password) }
    }

    private val _state = MutableStateFlow<AuthState>(
        signedInAs?.let { email ->
            AuthState.SignedIn(requireNotNull(users[email.lowercase()]) { "unknown seed $email" })
        } ?: AuthState.SignedOut,
    )
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    override suspend fun signUp(email: String, password: String): PennyUser? {
        if (!isValidEmail(email) || password.length < MIN_PASSWORD) return null
        if (users.containsKey(email.lowercase())) return null
        val user = create(email, password)
        _state.value = AuthState.SignedIn(user)
        return user
    }

    override suspend fun login(email: String, password: String): PennyUser? {
        val key = email.lowercase()
        if (!isValidEmail(email) || passwords[key] != password) return null
        val user = users.getValue(key)
        _state.value = AuthState.SignedIn(user)
        return user
    }

    override suspend fun signOut() {
        _state.value = AuthState.SignedOut
    }

    /** uid of a known email account (seeding helper). */
    fun uidOf(email: String): String? = users[email.lowercase()]?.uid

    private val claims = mutableMapOf<String, Map<String, String>>()

    /** Backend `/set-custom-claims` writes here (Firebase Admin stand-in). */
    fun setCustomClaims(uid: String, newClaims: Map<String, String>) {
        claims[uid] = newClaims
    }

    override suspend fun customClaims(): Map<String, String>? {
        val user = (state.value as? AuthState.SignedIn)?.user ?: return null
        return claims[user.uid] ?: emptyMap()
    }

    /**
     * One fixed fake account per provider. The first sign-in is
     * `isNewUser`; later ones return the custom claims like auth.dart.
     */
    override suspend fun signInWithProvider(provider: SocialProvider): SocialSignIn {
        val email = when (provider) {
            SocialProvider.GOOGLE -> GOOGLE_EMAIL
            SocialProvider.GITHUB -> GITHUB_EMAIL
        }
        val key = email.lowercase()
        val isNew = !users.containsKey(key)
        val user = users[key] ?: create(email, password = "")
        _state.value = AuthState.SignedIn(user)
        if (isNew) return SocialSignIn(user, isNewUser = true)
        val c = claims[user.uid]
        return SocialSignIn(user, isNewUser = false, username = c?.get("username"), fullName = c?.get("fullName"))
    }

    private var pendingPhone: String? = null

    /** E.164-ish numbers get a code (always [FAKE_SMS_CODE]); others fail. */
    override suspend fun verifyPhoneNumber(phoneNumber: String): Boolean {
        if (!PHONE.matches(phoneNumber)) return false
        pendingPhone = phoneNumber
        return true
    }

    override suspend fun signInWithSmsCode(smsCode: String): Boolean {
        val phone = pendingPhone ?: return false
        if (smsCode != FAKE_SMS_CODE) return false
        val key = "phone:$phone"
        val user = users[key] ?: PennyUser("fake-uid-${users.size + 1}", email = "").also { users[key] = it }
        _state.value = AuthState.SignedIn(user)
        return true
    }

    private fun create(email: String, password: String): PennyUser {
        val key = email.lowercase()
        val user = PennyUser(uid = "fake-uid-${users.size + 1}", email = email)
        passwords[key] = password
        users[key] = user
        return user
    }

    companion object {
        /** Firebase's minimum password length (`weak-password` below it). */
        const val MIN_PASSWORD = 6
        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        fun isValidEmail(email: String): Boolean = EMAIL.matches(email)

        const val GOOGLE_EMAIL = "google.user@gmail.com"
        const val GITHUB_EMAIL = "github.user@users.noreply.github.com"
        const val FAKE_SMS_CODE = "123456"
        private val PHONE = Regex("^\\+[1-9][0-9 ]{6,18}$")

        const val DEMO_EMAIL = "demo@penny.app"
        const val DEMO_PASSWORD = "penny123"

        /** What the app ships with until a real auth stack is chosen. */
        fun demo(): FakeAuthService = FakeAuthService(mapOf(DEMO_EMAIL to DEMO_PASSWORD))
    }
}
