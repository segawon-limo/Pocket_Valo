package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val username: String? = null,
    val puuid: String? = null,
    val playerCardUrl: String? = null,
    val accountLevel: Int? = null,
    val levelBorderUrl: String? = null,     // dari /v1/levelborders
    val rankName: String? = null,
    val rankIconUrl: String? = null,
    val titleText: String? = null,
    val isTitleLoading: Boolean = false,
    val error: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage    = TokenStorage(application)
    private val db              = AppDatabase.getInstance(application)
    private val authRepo        = RiotAuthRepository(tokenStorage)
    private val storeRepo       = StoreRepository(tokenStorage, authRepo, db.storeDao())
    private val assetsRepo      = AssetsRepository()

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState

    init {
        loadFromStorage()
    }

    private fun loadFromStorage() {
        val loggedIn = tokenStorage.isLoggedIn
        _uiState.value = _uiState.value.copy(
            isLoggedIn = loggedIn,
            username   = tokenStorage.username,
            puuid      = tokenStorage.puuid
        )
        if (loggedIn) fetchTitle()
    }

    fun setPlayerData(
        playerCardUrl: String?,
        accountLevel: Int?,
        rankName: String?,
        rankIconUrl: String?
    ) {
        _uiState.value = _uiState.value.copy(
            playerCardUrl = playerCardUrl,
            accountLevel  = accountLevel,
            rankName      = rankName,
            rankIconUrl   = rankIconUrl
        )
        // Fetch level border setelah accountLevel diketahui
        if (accountLevel != null) fetchLevelBorder(accountLevel)
    }

    private fun fetchLevelBorder(accountLevel: Int) {
        viewModelScope.launch {
            val border = assetsRepo.getLevelBorderForLevel(accountLevel)
            _uiState.value = _uiState.value.copy(
                levelBorderUrl = border?.smallPlayerCardAppearance
            )
        }
    }

    private fun fetchTitle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTitleLoading = true)
            when (val result = storeRepo.fetchPlayerTitle()) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        titleText      = result.data.titleText,
                        isTitleLoading = false
                    )
                }
                is AuthResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isTitleLoading = false)
                }
            }
        }
    }

    fun refresh() {
        loadFromStorage()
    }
}