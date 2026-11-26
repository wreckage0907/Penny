package com.penny.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Connectivity seam (artboard S04 "Budget · Offline + Loading"): offline
 * means "cached data + queued writes + retry", never an error screen.
 *
 * New behaviour — the Flutter app talked to Firebase without any offline
 * policy. The in-memory fake is the only implementation; a real one wraps
 * a ConnectivityManager/Firestore snapshot.
 */
interface Connectivity {
    val online: StateFlow<Boolean>
}

class FakeConnectivity(initial: Boolean = true) : Connectivity {
    private val _online = MutableStateFlow(initial)
    override val online: StateFlow<Boolean> = _online.asStateFlow()

    /** E2E lever: the test broadcast flips this (see MainActivity). */
    fun setOnline(value: Boolean) {
        _online.value = value
    }
}
