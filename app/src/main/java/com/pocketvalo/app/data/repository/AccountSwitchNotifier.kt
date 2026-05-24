package com.pocketvalo.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton event bus untuk memberi tahu ViewModel lain saat terjadi switch account.
 * Increment counter setiap switch — ViewModel subscribe dan reload saat nilai berubah.
 */
object AccountSwitchNotifier {
    private val _switchCount = MutableStateFlow(0)
    val switchCount: StateFlow<Int> = _switchCount.asStateFlow()

    fun notifySwitch() {
        _switchCount.value += 1
    }
}