package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreData
import com.pocketvalo.app.data.repository.StoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StoreUiState(
    val isLoading: Boolean = false,
    val store: StoreData? = null,
    val error: String? = null,
    val username: String? = null,
    val sessionExpired: Boolean = false  // signal LoginScreen navigation
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage   = TokenStorage(application)
    private val authRepository = RiotAuthRepository(tokenStorage)
    private val assetsRepository = AssetsRepository.getInstance()
    private val storeRepository  = StoreRepository(
        tokenStorage   = tokenStorage,
        authRepository = authRepository,
        storeDao       = AppDatabase.getInstance(application).storeDao()
    )

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(username = tokenStorage.username)
        loadStore()
    }

    fun loadStore(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = storeRepository.getStore(forceRefresh)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        store     = result.data
                    )
                }
                is AuthResult.Failure -> {
                    val errorMsg = result.error.message ?: "Failed to load store"
                    val needsRelogin = errorMsg.contains("revoked") ||
                            errorMsg.contains("Not logged in")
                    if (needsRelogin) {
                        authRepository.logout()
                        _uiState.value = _uiState.value.copy(
                            isLoading      = false,
                            sessionExpired = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error     = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    suspend fun getSkinInfo(levelUuid: String): AssetsRepository.StoreSkinInfo? {
        return withContext(Dispatchers.IO) {
            assetsRepository.getSkinByLevelUuid(levelUuid)
        }
    }
}