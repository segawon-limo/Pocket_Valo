


package com.pocketvalo.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.model.AccountData
import com.pocketvalo.app.data.model.MatchData
import com.pocketvalo.app.data.model.TierData
import com.pocketvalo.app.data.model.MapData
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.PlayerRepository
import com.pocketvalo.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.pocketvalo.app.BuildConfig
// Ganti dengan API key Henrik-3 kamu
//private const val API_KEY = "HDEV-your-api-key-here"
val API_KEY = BuildConfig.HENRIK_API_KEY

//private const val API_KEY = "HDEV-your-api-key-here"

data class PlayerUiState(
    val isLoading: Boolean = false,
    val accountData: AccountData? = null,
    val matchHistory: List<MatchData> = emptyList(),
    val rankTiers: Map<String, TierData> = emptyMap(),
    val maps: Map<String, MapData> = emptyMap(),
    val error: String? = null
)

class PlayerViewModel : ViewModel() {

    private val repository by lazy { PlayerRepository() }
    private val assetsRepository by lazy { AssetsRepository() }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    init {
        loadRankTiers()
    }

    private fun loadRankTiers() {
        viewModelScope.launch {
            when (val result = assetsRepository.getCompetitiveTiers()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(rankTiers = result.data)
                }
                else -> Unit
            }
        }
    }

    fun loadPlayerData(name: String, tag: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState(
                isLoading = true,
                rankTiers = _uiState.value.rankTiers,
                maps = _uiState.value.maps
            )

            when (val result = repository.getAccount(name, tag, API_KEY)) {
                is Result.Success -> {
                    val accountData = result.data.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accountData = accountData
                    )
                    accountData?.region?.let { region ->
                        loadMatchHistory(region, name, tag)
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> Unit
            }
        }
    }

    private fun loadMatchHistory(region: String, name: String, tag: String) {
        viewModelScope.launch {
            when (val result = repository.getMatchHistory(region, name, tag, API_KEY)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        matchHistory = result.data.data ?: emptyList()
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                else -> Unit
            }
        }
    }

    init {
        loadAssets()
    }

    private fun loadAssets() {
        viewModelScope.launch {
            when (val result = assetsRepository.getCompetitiveTiers()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(rankTiers = result.data)
                else -> Unit
            }
        }
        viewModelScope.launch {
            when (val result = assetsRepository.getMaps()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(maps = result.data)
                else -> Unit
            }
        }
    }
}