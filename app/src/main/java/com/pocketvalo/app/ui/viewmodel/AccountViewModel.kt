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
    val username: String? = null,
    val puuid: String? = null,
    val playerCardUrl: String? = null,
    val accountLevel: Int? = null,
    val levelBorderUrl: String? = null,
    val rankName: String? = null,
    val rankIconUrl: String? = null,
    val titleText: String? = null,
    val isTitleLoading: Boolean = false,
    val error: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val db           = AppDatabase.getInstance(application)
    private val authRepo     = RiotAuthRepository(tokenStorage)
    private val storeRepo    = StoreRepository(tokenStorage, authRepo, db.storeDao())
    private val assetsRepo   = AssetsRepository.getInstance()  // shared singleton

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(
            username = tokenStorage.username,
            puuid    = tokenStorage.puuid
        )
        fetchTitle()
    }

    fun setPlayerData(
        playerCardUrl: String?,
        accountLevel: Int?,
        rankName: String?,
        rankIconUrl: String?
    ) {
        _uiState.value = _uiState.value.copy(
            // Prefer Henrik data kalau ada, jangan overwrite dengan null
            playerCardUrl = playerCardUrl ?: _uiState.value.playerCardUrl,
            accountLevel  = accountLevel  ?: _uiState.value.accountLevel,
            rankName      = rankName,
            rankIconUrl   = rankIconUrl
        )
        val resolvedLevel = accountLevel ?: _uiState.value.accountLevel
        if (resolvedLevel != null) fetchLevelBorder(resolvedLevel)
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
                    val info = result.data
                    // Construct player card large art URL from loadout UUID
                    // Tidak butuh Henrik API — URL deterministik dari valorant-api.com CDN
                    val cardUrl = info.playerCardId
                        ?.takeIf { it != "00000000-0000-0000-0000-000000000000" }
                        ?.let { "https://media.valorant-api.com/playercards/$it/largeart.png" }

                    _uiState.value = _uiState.value.copy(
                        titleText      = info.titleText,
                        isTitleLoading = false,
                        // Hanya overwrite playerCardUrl kalau Henrik belum set (accountData belum ada)
                        playerCardUrl  = _uiState.value.playerCardUrl ?: cardUrl,
                        // Hanya overwrite accountLevel kalau belum dapat dari Henrik
                        accountLevel   = _uiState.value.accountLevel ?: info.accountLevel
                    )
                }
                is AuthResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isTitleLoading = false)
                }
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(
            username = tokenStorage.username,
            puuid    = tokenStorage.puuid
        )
        fetchTitle()
    }
}