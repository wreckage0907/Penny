package com.penny.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Learning-progress seam: [LearnState] behind a StateFlow so every screen
 * (Learn path, Lesson, Quiz, Results, Profile achievements) sees the same
 * journey. New behaviour (v2.1); the fake is the only implementation until
 * a backend is chosen.
 */
interface LearnStore {
    val state: StateFlow<LearnState>
    suspend fun set(state: LearnState)
}

class InMemoryLearnStore(initial: LearnState = LearnState()) : LearnStore {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<LearnState> = _state.asStateFlow()
    override suspend fun set(state: LearnState) {
        _state.value = state
    }
}

object LearnStores {
    /**
     * The demo journey (artboard frame 09): 240 coins, 5-day streak,
     * lessons 1–2 of Personal Finance complete, no badges yet.
     */
    fun demo(today: java.time.LocalDate = java.time.LocalDate.now()): LearnState =
        LearnState(
            coins = 240,
            streakDays = 5,
            completed = setOf(
                LessonKey(FakeCourseContent.SAMPLE.keys.first(), 0),
                LessonKey(FakeCourseContent.SAMPLE.keys.first(), 1),
            ),
            lastActiveDay = today,
        )
}
