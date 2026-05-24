package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.BuildConfig
import com.pocketvalo.app.data.local.entity.AccountEntity
import com.pocketvalo.app.data.local.entity.MatchEntity
import com.pocketvalo.app.data.model.AccountData
import com.pocketvalo.app.data.model.MapData
import com.pocketvalo.app.data.model.TierData
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.PlayerRepository
import com.pocketvalo.app.data.repository.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val API_KEY = BuildConfig.HENRIK_API_KEY

data class PlayerUiState(
    val isLoading: Boolean = false,
    val accountData: AccountData? = null,
    val playerCardSmallUrl: String? = null,
    val matchHistory: List<MatchEntity> = emptyList(),
    val rawMatchHistory: List<com.pocketvalo.app.data.model.MatchData> = emptyList(),
    val selectedMatch: com.pocketvalo.app.data.model.MatchData? = null,
    val matchDetail: com.pocketvalo.app.data.model.MatchDetailData? = null,
    val isLoadingDetail: Boolean = false,
    val rankTiers: Map<String, TierData> = emptyMap(),
    val maps: Map<String, MapData> = emptyMap(),
    val savedAccounts: List<AccountEntity> = emptyList(),
    // MMR — current rank dari Henrik MMR endpoint
    val currentRankName: String? = null,
    val currentRankIconUrl: String? = null,
    val currentRR: Int? = null,
    val error: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlayerRepository(application)
    private val assetsRepository = AssetsRepository.getInstance()
    private val tokenStorage = TokenStorage(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    // Currently active Riot ID — used to observe correct match history flow
    private var activeRiotId: String? = null

    init {
        observeSavedAccounts()
    }

    /**
     * Called by HomeScreen on mount when user came through the Riot auth flow
     * (i.e. no InputScreen was visited). Reads gameName#tagLine from TokenStorage
     * and triggers the full player data load.
     * No-op if accountData is already loaded or username isn't stored.
     */
    fun loadFromTokenIfNeeded() {
        if (_uiState.value.accountData != null) return
        if (_uiState.value.isLoading) return
        val username = tokenStorage.username ?: return
        val parts = username.split("#")
        if (parts.size != 2) return
        val name = parts[0].trim()
        val tag  = parts[1].trim()
        if (name.isBlank() || tag.isBlank()) return
        loadPlayerData(name, tag)
    }

    private fun observeSavedAccounts() {
        viewModelScope.launch {
            repository.getSavedAccounts().collect { accounts ->
                _uiState.value = _uiState.value.copy(savedAccounts = accounts)
            }
        }
    }

    private fun observeMatchHistory(riotId: String) {
        if (activeRiotId == riotId) return     // already observing
        activeRiotId = riotId
        viewModelScope.launch {
            repository.getCachedMatchHistory(riotId).collect { matches ->
                val filtered = matches
                    .take(15)   // ambil 15 match terakhir dulu
                    .filter { it.mode.equals("Competitive", ignoreCase = true) ||
                            it.mode.equals("Unrated", ignoreCase = true) }
                    .take(10)   // lalu ambil 10 Competitive/Unrated
                _uiState.value = _uiState.value.copy(matchHistory = filtered)
            }
        }
    }

    fun loadPlayerData(name: String, tag: String) {
        val riotId = "$name#$tag"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                rankTiers = _uiState.value.rankTiers,
                maps = _uiState.value.maps
            )

            // Start observing cache immediately — UI shows stale data while refreshing
            observeMatchHistory(riotId)

            when (val result = repository.getAccount(name, tag, API_KEY)) {
                is Result.Success -> {
                    val accountData = result.data.data
                    _uiState.value = _uiState.value.copy(
                        isLoading          = false,
                        accountData        = accountData,
                        playerCardSmallUrl = accountData?.card?.small
                    )
                    // Load map/rank assets now that user has logged in
                    loadAssetsIfNeeded()
                    accountData?.region?.let { region ->
                        refreshMatchHistory(region, name, tag)
                        loadMmr(region, name, tag)
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

    private fun refreshMatchHistory(region: String, name: String, tag: String) {
        viewModelScope.launch {
            val result = repository.refreshMatchHistory(region, name, tag, API_KEY)
            if (result is Result.Success) {
                // Also keep raw data in-memory for MatchScreen scoreboard this session
                _uiState.value = _uiState.value.copy(
                    rawMatchHistory = result.data.data ?: emptyList()
                )
            }
        }
    }

    fun loadMmr(region: String, name: String, tag: String) {
        viewModelScope.launch {
            when (val result = repository.getMmr(region, name, tag, API_KEY)) {
                is Result.Success -> {
                    val current = result.data.currentData
                    _uiState.value = _uiState.value.copy(
                        currentRankName    = current?.currentTierPatched,
                        currentRankIconUrl = current?.images?.small,
                        currentRR          = current?.rankingInTier
                    )
                }
                else -> Unit  // silent fail — HomeScreen tetap tampil tanpa rank
            }
        }
    }

    fun deleteAccount(riotId: String) {
        viewModelScope.launch {
            repository.deleteAccount(riotId)
            if (activeRiotId == riotId) {
                activeRiotId = null
                _uiState.value = _uiState.value.copy(
                    accountData = null,
                    matchHistory = emptyList()
                )
            }
        }
    }

    private fun loadAssetsIfNeeded() {
        if (_uiState.value.rankTiers.isNotEmpty() && _uiState.value.maps.isNotEmpty()) return
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

    fun loadMatchDetail(matchId: String) {
        // Skip if already loaded for this match
        if (_uiState.value.matchDetail?.metadata?.matchId == matchId) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingDetail = true,
                matchDetail = null
            )
            when (val result = repository.getMatchDetail(matchId, API_KEY)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetail = false,
                        matchDetail = result.data.data
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingDetail = false)
                }
                else -> Unit
            }
        }
    }
}