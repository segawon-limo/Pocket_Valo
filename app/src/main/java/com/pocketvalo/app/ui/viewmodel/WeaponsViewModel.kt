package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.model.WeaponData
import com.pocketvalo.app.data.repository.AccountSwitchNotifier
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.Result
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WeaponsUiState(
    val isLoading: Boolean = false,
    val weapons: List<WeaponData> = emptyList(),
    val filteredWeapons: List<WeaponData> = emptyList(),
    val selectedCategory: String? = null,
    val equippedSkins: Map<String, String> = emptyMap(),
    val error: String? = null
)

class WeaponsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository   = AssetsRepository.getInstance()
    private val tokenStorage = TokenStorage(application)
    private val multiStorage = MultiAccountTokenStorage(application)
    private val storeRepo    = StoreRepository(
        tokenStorage   = tokenStorage,
        authRepository = RiotAuthRepository(tokenStorage, multiStorage),
        storeDao       = AppDatabase.getInstance(application).storeDao()
    )

    private val _uiState = MutableStateFlow(WeaponsUiState())
    val uiState: StateFlow<WeaponsUiState> = _uiState

    val categoryOrder = listOf("Melee", "Sidearm", "SMG", "Shotgun", "Rifle", "Sniper", "Heavy")

    init {
        loadWeaponsIfNeeded()
        fetchEquippedSkins()
        observeAccountSwitch()
    }

    private fun observeAccountSwitch() {
        viewModelScope.launch {
            AccountSwitchNotifier.switchCount.collect { count ->
                if (count > 0) {
                    // Clear equipped skins untuk akun lama, fetch ulang untuk akun baru
                    _uiState.value = _uiState.value.copy(equippedSkins = emptyMap())
                    fetchEquippedSkins()
                }
            }
        }
    }

    fun loadWeaponsIfNeeded() {
        if (_uiState.value.weapons.isNotEmpty() || _uiState.value.isLoading) return
        loadWeapons()
    }

    private fun loadWeapons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.getWeapons()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading       = false,
                        weapons         = result.data,
                        filteredWeapons = result.data
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error     = result.message
                    )
                }
                else -> Unit
            }
        }
    }

    fun fetchEquippedSkins() {
        if (!tokenStorage.isLoggedIn) return
        viewModelScope.launch {
            when (val result = storeRepo.fetchEquippedSkins()) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(equippedSkins = result.data)
                }
                is AuthResult.Failure -> { /* silent fail */ }
            }
        }
    }

    fun filterByCategory(category: String?) {
        val weapons = _uiState.value.weapons
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            filteredWeapons  = if (category == null) weapons
            else weapons.filter { it.category.contains(category, ignoreCase = true) }
        )
    }
}