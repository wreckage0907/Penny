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
}

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

        const val DEMO_EMAIL = "demo@penny.app"
        const val DEMO_PASSWORD = "penny123"

        /** What the app ships with until a real auth stack is chosen. */
        fun demo(): FakeAuthService = FakeAuthService(mapOf(DEMO_EMAIL to DEMO_PASSWORD))
    }
}
