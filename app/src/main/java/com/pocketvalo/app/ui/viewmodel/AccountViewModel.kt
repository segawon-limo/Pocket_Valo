package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.entity.MatchEntity
import com.pocketvalo.app.data.model.MatchData
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerStats(
    val totalMatches: Int       = 0,
    val wins: Int               = 0,
    val winRate: Float          = 0f,
    val avgKills: Float         = 0f,
    val avgDeaths: Float        = 0f,
    val avgAssists: Float       = 0f,
    val kdRatio: Float          = 0f,
    val headshotPct: Float      = 0f,   // 0.0–1.0
    val bodyshotPct: Float      = 0f,
    val legshotPct: Float       = 0f,
    val mostPlayedAgent: String?    = null,
    val mostPlayedAgentUrl: String? = null,
    val mostPlayedAgentGames: Int   = 0,
    val mostPlayedMap: String?      = null,
    val mostPlayedMapImageUrl: String? = null,
    val mostPlayedMapWins: Int      = 0,
    val mostPlayedMapLosses: Int    = 0
)

data class AccountUiState(
    val isLoading: Boolean = false,
    val username: String? = null,
    val puuid: String? = null,
    val playerCardUrl: String? = null,
    val accountLevel: Int? = null,
    val levelBorderUrl: String? = null,
    val levelNumberAppearanceUrl: String? = null,
    val rankName: String? = null,
    val rankIconUrl: String? = null,
    val currentRR: Int? = null,
    val titleText: String? = null,
    val isTitleLoading: Boolean = false,
    val stats: PlayerStats? = null,
    val error: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val db           = AppDatabase.getInstance(application)
    private val authRepo     = RiotAuthRepository(tokenStorage)
    private val storeRepo    = StoreRepository(tokenStorage, authRepo, db.storeDao())
    private val assetsRepo   = AssetsRepository.getInstance()

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
        rankIconUrl: String?,
        currentRR: Int? = null,
        matchHistory: List<MatchEntity> = emptyList(),
        rawMatchHistory: List<MatchData> = emptyList(),
        maps: Map<String, com.pocketvalo.app.data.model.MapData> = emptyMap()
    ) {
        val resolvedLevel = accountLevel ?: _uiState.value.accountLevel
        _uiState.value = _uiState.value.copy(
            playerCardUrl = playerCardUrl ?: _uiState.value.playerCardUrl,
            accountLevel  = resolvedLevel,
            rankName      = rankName,
            rankIconUrl   = rankIconUrl,
            currentRR     = currentRR,
            stats         = if (matchHistory.isNotEmpty())
                computeStats(matchHistory, rawMatchHistory, maps)
            else _uiState.value.stats
        )
        if (resolvedLevel != null) fetchLevelBorder(resolvedLevel)
    }

    private fun computeStats(
        matchHistory: List<MatchEntity>,
        rawMatchHistory: List<MatchData>,
        maps: Map<String, com.pocketvalo.app.data.model.MapData> = emptyMap()
    ): PlayerStats {
        if (matchHistory.isEmpty()) return PlayerStats()

        val total  = matchHistory.size
        val wins   = matchHistory.count { it.hasWon }

        val avgKills   = matchHistory.map { it.kills }.average().toFloat()
        val avgDeaths  = matchHistory.map { it.deaths }.average().toFloat()
        val avgAssists = matchHistory.map { it.assists }.average().toFloat()
        val kdRatio    = if (avgDeaths > 0) avgKills / avgDeaths else avgKills

        // Shot distribution dari rawMatchHistory
        val username = tokenStorage.username
        var totalHeadshots = 0
        var totalBodyshots = 0
        var totalShots     = 0

        if (rawMatchHistory.isNotEmpty() && username != null) {
            rawMatchHistory.forEach { matchData ->
                val player = matchData.players?.allPlayers
                    ?.firstOrNull { "${it.name}#${it.tag}" == username }
                val stats = player?.stats
                if (stats != null) {
                    totalHeadshots += stats.headshots
                    totalBodyshots += stats.bodyshots
                    totalShots     += stats.headshots + stats.bodyshots
                }
            }
        }

        val headshotPct = if (totalShots > 0) totalHeadshots.toFloat() / totalShots else 0f
        val bodyshotPct = if (totalShots > 0) totalBodyshots.toFloat() / totalShots else 0f
        val legshotPct  = (1f - headshotPct - bodyshotPct).coerceAtLeast(0f)

        // Most played agent
        val agentCounts  = matchHistory.groupingBy { it.agentName }.eachCount()
        val topAgentName = agentCounts.maxByOrNull { it.value }?.key
        val topAgentUrl  = matchHistory.firstOrNull { it.agentName == topAgentName }?.agentPortraitUrl
        val topAgentGames = agentCounts[topAgentName] ?: 0

        // Most played map + image + win/loss
        val mapCounts   = matchHistory.groupingBy { it.map }.eachCount()
        val topMapName  = mapCounts.maxByOrNull { it.value }?.key
        val mapMatches  = matchHistory.filter { it.map == topMapName }
        val mapWins     = mapMatches.count { it.hasWon }
        val mapLosses   = mapMatches.size - mapWins
        // Lookup map image dari AssetsRepository cache (key = displayName uppercase)
        val mapImageUrl = topMapName?.let { name ->
            maps[name.uppercase()]?.listViewIcon
        }

        return PlayerStats(
            totalMatches          = total,
            wins                  = wins,
            winRate               = wins.toFloat() / total,
            avgKills              = avgKills,
            avgDeaths             = avgDeaths,
            avgAssists            = avgAssists,
            kdRatio               = kdRatio,
            headshotPct           = headshotPct,
            bodyshotPct           = bodyshotPct,
            legshotPct            = legshotPct,
            mostPlayedAgent       = topAgentName,
            mostPlayedAgentUrl    = topAgentUrl,
            mostPlayedAgentGames  = topAgentGames,
            mostPlayedMap         = topMapName,
            mostPlayedMapImageUrl = mapImageUrl,
            mostPlayedMapWins     = mapWins,
            mostPlayedMapLosses   = mapLosses
        )
    }

    private fun fetchLevelBorder(accountLevel: Int) {
        viewModelScope.launch {
            val border = assetsRepo.getLevelBorderForLevel(accountLevel)
            _uiState.value = _uiState.value.copy(
                levelBorderUrl           = border?.smallPlayerCardAppearance,
                levelNumberAppearanceUrl = border?.levelNumberAppearance
            )
        }
    }

    private fun fetchTitle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTitleLoading = true)
            when (val result = storeRepo.fetchPlayerTitle()) {
                is AuthResult.Success -> {
                    val info    = result.data
                    val cardUrl = info.playerCardId
                        ?.takeIf { it != "00000000-0000-0000-0000-000000000000" }
                        ?.let { "https://media.valorant-api.com/playercards/$it/largeart.png" }
                    _uiState.value = _uiState.value.copy(
                        titleText      = info.titleText,
                        isTitleLoading = false,
                        playerCardUrl  = _uiState.value.playerCardUrl ?: cardUrl,
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