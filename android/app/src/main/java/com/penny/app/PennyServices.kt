package com.penny.app

import com.penny.app.core.AuthService
import com.penny.app.core.FakeAuthService

/**
 * Everything the screens talk to, behind interfaces. Until the maintainer
 * picks a backend/auth stack every entry is a fake that mimics the current
 * services; tests build their own instance.
 */
class PennyServices(val auth: AuthService) {
    companion object {
        fun fakes(): PennyServices = PennyServices(auth = FakeAuthService.demo())
    }
}

/** Process-wide instance used by MainActivity (survives configuration changes). */
object AppGraph {
    val services: PennyServices by lazy { PennyServices.fakes() }
}
