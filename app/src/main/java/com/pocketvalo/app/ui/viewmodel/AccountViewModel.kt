package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.entity.AccountEntity
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
    val headshotPct: Float      = 0f,
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
    val error: String? = null,
    // Switch account
    val savedAccounts: List<AccountEntity> = emptyList(),
    val showAccountSheet: Boolean = false,
    val isSwitching: Boolean = false,
    val switchError: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val multiStorage = MultiAccountTokenStorage(application)
    private val db           = AppDatabase.getInstance(application)
    private val authRepo     = RiotAuthRepository(tokenStorage, multiStorage)
    private val storeRepo    = StoreRepository(tokenStorage, authRepo, db.storeDao())
    private val assetsRepo   = AssetsRepository.getInstance()

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState

    // Callback dipanggil saat token sudah di-swap — AppNavigation navigate ke LoadingScreen
    var onNavigateToLoading: (() -> Unit)? = null

    init {
        _uiState.value = _uiState.value.copy(
            username = tokenStorage.username,
            puuid    = tokenStorage.puuid
        )
        fetchTitle()
        observeSavedAccounts()

        // Migrate active account ke MultiStorage jika belum ada
        migrateActiveAccountIfNeeded()
    }

    private fun migrateActiveAccountIfNeeded() {
        val puuid = tokenStorage.puuid ?: return
        if (multiStorage.getKnownPuuids().isEmpty() || multiStorage.getRefreshToken(puuid) == null) {
            val accessToken  = tokenStorage.accessToken ?: return
            val refreshToken = tokenStorage.refreshToken ?: return
            val entitlement  = tokenStorage.entitlementToken ?: return
            val region       = tokenStorage.region ?: return
            val username     = tokenStorage.username ?: return
            multiStorage.saveAccount(
                puuid            = puuid,
                accessToken      = accessToken,
                idToken          = tokenStorage.idToken ?: "",
                refreshToken     = refreshToken,
                entitlementToken = entitlement,
                region           = region,
                username         = username,
                expiresInSeconds = ((tokenStorage.accessTokenExpiresAt - System.currentTimeMillis()) / 1000)
                    .toInt().coerceAtLeast(0)
            )
            if (multiStorage.activePuuid == null) multiStorage.activePuuid = puuid
        }
    }

    private fun observeSavedAccounts() {
        viewModelScope.launch {
            db.accountDao().getAllAccounts().collect { accounts ->
                _uiState.value = _uiState.value.copy(savedAccounts = accounts)
            }
        }
    }

    // ── Sheet control ─────────────────────────────────────────────────────────

    fun showAccountSheet()  { _uiState.value = _uiState.value.copy(showAccountSheet = true,  switchError = null) }
    fun dismissAccountSheet() { _uiState.value = _uiState.value.copy(showAccountSheet = false) }

    // ── Switch account ────────────────────────────────────────────────────────

    fun switchToAccount(account: AccountEntity) {
        if (account.puuid == tokenStorage.puuid) {
            dismissAccountSheet()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSwitching = true, switchError = null)

            // Pastikan token akun target masih valid
            val tokenResult = authRepo.ensureValidTokenForPuuid(account.puuid)
            if (tokenResult is AuthResult.Failure) {
                _uiState.value = _uiState.value.copy(
                    isSwitching = false,
                    switchError = "Session expired for ${account.gameName}. Please re-add this account."
                )
                return@launch
            }

            // Commit switch: tulis token akun target ke TokenStorage aktif
            val puuid = account.puuid
            multiStorage.activePuuid = puuid
            tokenStorage.puuid    = puuid
            tokenStorage.region   = multiStorage.getRegion(puuid)
            tokenStorage.username = multiStorage.getUsername(puuid)
            multiStorage.getAccessToken(puuid)?.let { at ->
                tokenStorage.saveTokens(
                    accessToken      = at,
                    idToken          = multiStorage.getIdToken(puuid) ?: "",
                    refreshToken     = multiStorage.getRefreshToken(puuid) ?: "",
                    expiresInSeconds = ((multiStorage.getAccessExpiresAt(puuid) - System.currentTimeMillis()) / 1000)
                        .toInt().coerceAtLeast(0)
                )
            }
            multiStorage.getEntitlementToken(puuid)?.let { tokenStorage.entitlementToken = it }

            // Tutup sheet
            _uiState.value = _uiState.value.copy(
                isSwitching      = false,
                showAccountSheet = false
            )

            // Beritahu Store & Weapons untuk reload data akun baru
            com.pocketvalo.app.data.repository.AccountSwitchNotifier.notifySwitch()

            // Navigate ke LoadingScreen — semua data di-fetch ulang dari awal
            onNavigateToLoading?.invoke()
        }
    }

    fun removeAccount(account: AccountEntity) {
        viewModelScope.launch {
            multiStorage.removeAccount(account.puuid)
            db.accountDao().deleteAccount(account.riotId)
        }
    }

    // ── setPlayerData (unchanged from before) ─────────────────────────────────

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

        val agentCounts   = matchHistory.groupingBy { it.agentName }.eachCount()
        val topAgentName  = agentCounts.maxByOrNull { it.value }?.key
        val topAgentUrl   = matchHistory.firstOrNull { it.agentName == topAgentName }?.agentPortraitUrl
        val topAgentGames = agentCounts[topAgentName] ?: 0

        val mapCounts  = matchHistory.groupingBy { it.map }.eachCount()
        val topMapName = mapCounts.maxByOrNull { it.value }?.key
        val mapMatches = matchHistory.filter { it.map == topMapName }
        val mapWins    = mapMatches.count { it.hasWon }
        val mapLosses  = mapMatches.size - mapWins
        val mapImageUrl = topMapName?.let { maps[it.uppercase()]?.listViewIcon }

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

    fun fetchTitle() {
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

    /**
     * Sync uiState dengan TokenStorage aktif saat ini.
     * Dipanggil setiap kali AccountScreen di-enter — penting setelah switch account
     * karena AccountViewModel tidak re-create (shared ViewModel).
     */
    fun refresh() {
        val activePuuid = multiStorage.activePuuid ?: tokenStorage.puuid
        _uiState.value = _uiState.value.copy(
            username  = tokenStorage.username,
            puuid     = activePuuid,
            // Reset data akun lama supaya tidak tampil stale
            playerCardUrl            = null,
            accountLevel             = null,
            levelBorderUrl           = null,
            levelNumberAppearanceUrl = null,
            rankName                 = null,
            rankIconUrl              = null,
            currentRR                = null,
            titleText                = null,
            stats                    = null
        )
        fetchTitle()
    }
}