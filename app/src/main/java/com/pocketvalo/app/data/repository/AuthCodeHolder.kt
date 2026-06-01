package com.pocketvalo.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton untuk meneruskan auth code dari MainActivity.onNewIntent
 * ke LoginScreen yang sedang menunggu.
 *
 * Flow:
 * 1. Chrome Custom Tab redirect ke pocketvalo://auth?code=xxx
 * 2. Android trigger MainActivity.onNewIntent dengan Intent berisi URI tersebut
 * 3. MainActivity extract code dan set ke AuthCodeHolder.emit(code)
 * 4. LoginScreen yang subscribe via collect() mendapat code dan proses login
 */
object AuthCodeHolder {
    private val _authCode = MutableStateFlow<String?>(null)
    val authCode: StateFlow<String?> = _authCode.asStateFlow()

    fun emit(code: String) {
        _authCode.value = code
    }

    fun clear() {
        _authCode.value = null
    }
}