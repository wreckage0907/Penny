package com.penny.app

import com.penny.app.core.AuthService
import com.penny.app.core.FakeAuthService
import com.penny.app.core.FakeProfileImages
import com.penny.app.core.FakeUserDirectory
import com.penny.app.core.InMemoryUserPrefs
import com.penny.app.core.ProfileImages
import com.penny.app.core.UserDirectory
import com.penny.app.core.UserPrefs
import kotlinx.coroutines.runBlocking

/**
 * Everything the screens talk to, behind interfaces. Until the maintainer
 * picks a backend/auth stack every entry is a fake that mimics the current
 * services; tests build their own instance.
 */
class PennyServices(
    val auth: AuthService,
    val users: UserDirectory,
    val prefs: UserPrefs = InMemoryUserPrefs(),
    val profileImages: ProfileImages = FakeProfileImages(),
) {
    companion object {
        /** Demo world: account demo@penny.app / penny123, onboarded as "demo". */
        fun fakes(signedIn: Boolean = false): PennyServices {
            val auth = FakeAuthService(
                mapOf(FakeAuthService.DEMO_EMAIL to FakeAuthService.DEMO_PASSWORD),
                signedInAs = if (signedIn) FakeAuthService.DEMO_EMAIL else null,
            )
            val users = runBlocking { FakeUserDirectory.demo(auth) }
            return PennyServices(auth, users)
        }
    }
}

/** Process-wide instance used by MainActivity (survives configuration changes). */
object AppGraph {
    val services: PennyServices by lazy { PennyServices.fakes() }
}
