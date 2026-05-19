package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreData
import com.pocketvalo.app.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StoreUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val showAuthWebView: Boolean = false,
    val authUrl: String? = null,
    val store: StoreData? = null,
    val error: String? = null,
    val username: String? = null
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val authRepository = RiotAuthRepository(tokenStorage)
    private val storeRepository = StoreRepository(
        tokenStorage = tokenStorage,
        authRepository = authRepository,
        storeDao = AppDatabase.getInstance(application).storeDao()
    )

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState

    init {
        checkLoginState()
    }

    private fun checkLoginState() {
        val loggedIn = tokenStorage.isLoggedIn
        _uiState.value = _uiState.value.copy(
            isLoggedIn = loggedIn,
            username = tokenStorage.username
        )
        if (loggedIn) loadStore()
    }

    fun startLogin() {
        val url = authRepository.generateAuthUrl()
        _uiState.value = _uiState.value.copy(
            authUrl = url,
            showAuthWebView = true,
            error = null
        )
    }

    fun dismissAuthWebView() {
        _uiState.value = _uiState.value.copy(
            showAuthWebView = false,
            authUrl = null
        )
    }

    fun handleAuthCode(code: String) {
        android.util.Log.d("StoreViewModel", "handleAuthCode called, code length: ${code.length}")
        _uiState.value = _uiState.value.copy(
            showAuthWebView = false,
            authUrl = null,
            isLoading = true,
            error = null
        )
        viewModelScope.launch {
            android.util.Log.d("StoreViewModel", "Calling loginWithCode...")
            when (val result = authRepository.loginWithCode(code)) {
                is AuthResult.Success -> {
                    android.util.Log.d("StoreViewModel", "Login success, username: ${tokenStorage.username}")
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        username = tokenStorage.username
                    )
                    loadStore()
                }
                is AuthResult.Failure -> {
                    android.util.Log.e("StoreViewModel", "Login failed: ${result.error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.error.message ?: "Login failed"
                    )
                }
            }
        }
    }

    fun loadStore(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = storeRepository.getStore(forceRefresh)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        store = result.data
                    )
                }
                is AuthResult.Failure -> {
                    val errorMsg = result.error.message ?: "Failed to load store"
                    val needsRelogin = errorMsg.contains("revoked") ||
                            errorMsg.contains("Not logged in")
                    if (needsRelogin) {
                        authRepository.logout()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            store = null,
                            error = "Session expired. Please login again."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = StoreUiState(isLoggedIn = false)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}