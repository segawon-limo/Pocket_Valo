package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.entity.WatchlistEntity
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.worker.WatchlistWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val MAX_WATCHLIST = 3

data class BrowseSkinItem(
    val skinUuid: String,
    val levelUuid: String,
    val displayName: String,
    val iconUrl: String?,
    val weaponType: String?,
    val tierUuid: String?
)

data class WatchlistUiState(
    val watchlist: List<WatchlistEntity> = emptyList(),
    val browseSkins: List<BrowseSkinItem> = emptyList(),
    val filteredSkins: List<BrowseSkinItem> = emptyList(),
    val selectedFilter: String? = null,
    val isLoadingSkins: Boolean = false,
    val isFull: Boolean = false,
    val error: String? = null
)

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {

    private val db           = AppDatabase.getInstance(application)
    private val tokenStorage = TokenStorage(application)
    private val multiStorage = MultiAccountTokenStorage(application)
    private val assetsRepo   = AssetsRepository.getInstance()

    private val activePuuid: String
        get() = multiStorage.activePuuid ?: tokenStorage.puuid ?: ""

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState

    val weaponFilters = listOf("Melee", "Sidearm", "SMG", "Shotgun", "Rifle", "Sniper", "Heavy")

    init {
        observeWatchlist()
        loadBrowseSkins()
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            db.watchlistDao().getWatchlistForAccount(activePuuid).collect { list ->
                _uiState.value = _uiState.value.copy(
                    watchlist = list,
                    isFull    = list.size >= MAX_WATCHLIST
                )
            }
        }
    }

    private fun loadBrowseSkins() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSkins = true)
            try {
                val weapons = assetsRepo.getWeapons()
                if (weapons is com.pocketvalo.app.data.repository.Result.Success) {
                    val skins = weapons.data.flatMap { weapon ->
                        val weaponType = weapon.category
                            .substringAfterLast("::")
                            .replaceFirstChar { it.uppercase() }
                        weapon.skins
                            .filter { skin ->
                                // Skip skins default (Standard) dan levelnya kosong
                                !skin.displayName.contains("Standard", ignoreCase = true) &&
                                        skin.levels.isNotEmpty()
                            }
                            .map { skin ->
                                BrowseSkinItem(
                                    skinUuid    = skin.uuid,
                                    levelUuid   = skin.levels.first().uuid,
                                    displayName = skin.displayName,
                                    iconUrl     = skin.levels.first().displayIcon
                                        ?: skin.displayIcon,
                                    weaponType  = weaponType,
                                    tierUuid    = skin.contentTierUuid
                                )
                            }
                    }.sortedBy { it.displayName }

                    _uiState.value = _uiState.value.copy(
                        browseSkins     = skins,
                        filteredSkins   = skins,
                        isLoadingSkins  = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingSkins = false,
                    error = e.message
                )
            }
        }
    }

    fun filterByWeapon(type: String?) {
        val all = _uiState.value.browseSkins
        _uiState.value = _uiState.value.copy(
            selectedFilter = type,
            filteredSkins  = if (type == null) all
            else all.filter { it.weaponType.equals(type, ignoreCase = true) }
        )
    }

    fun addToWatchlist(skin: BrowseSkinItem) {
        if (_uiState.value.isFull) return
        viewModelScope.launch {
            val alreadyIn = db.watchlistDao().isInWatchlist(activePuuid, skin.skinUuid)
            if (alreadyIn) return@launch

            db.watchlistDao().insert(
                WatchlistEntity(
                    id          = "$activePuuid:${skin.skinUuid}",
                    puuid       = activePuuid,
                    skinUuid    = skin.skinUuid,
                    levelUuid   = skin.levelUuid,
                    displayName = skin.displayName,
                    iconUrl     = skin.iconUrl,
                    weaponType  = skin.weaponType,
                    tierUuid    = skin.tierUuid
                )
            )

            // Schedule WorkManager saat pertama kali ada item di watchlist
            WatchlistWorker.schedule(getApplication())
        }
    }

    fun removeFromWatchlist(item: WatchlistEntity) {
        viewModelScope.launch {
            db.watchlistDao().deleteBySkin(activePuuid, item.skinUuid)
            // Cancel worker kalau watchlist kosong
            val remaining = db.watchlistDao().countForAccount(activePuuid)
            if (remaining == 0) WatchlistWorker.cancel(getApplication())
        }
    }

    fun removeFromWatchlistBySkinUuid(skinUuid: String) {
        viewModelScope.launch {
            db.watchlistDao().deleteBySkin(activePuuid, skinUuid)
            val remaining = db.watchlistDao().countForAccount(activePuuid)
            if (remaining == 0) WatchlistWorker.cancel(getApplication())
        }
    }

    suspend fun isInWatchlist(skinUuid: String): Boolean =
        db.watchlistDao().isInWatchlist(activePuuid, skinUuid)
}