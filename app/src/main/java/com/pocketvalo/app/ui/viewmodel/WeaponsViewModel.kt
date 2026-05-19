package com.pocketvalo.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.model.WeaponData
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WeaponsUiState(
    val isLoading: Boolean = false,
    val weapons: List<WeaponData> = emptyList(),
    val filteredWeapons: List<WeaponData> = emptyList(),
    val selectedCategory: String? = null,
    val error: String? = null
)

class WeaponsViewModel : ViewModel() {

    private val repository = AssetsRepository()

    private val _uiState = MutableStateFlow(WeaponsUiState())
    val uiState: StateFlow<WeaponsUiState> = _uiState

    val categoryOrder = listOf("Melee", "Sidearm", "SMG", "Shotgun", "Rifle", "Sniper", "Heavy")

    init {
        loadWeaponsIfNeeded()
    }

    fun loadWeaponsIfNeeded() {
        if (_uiState.value.weapons.isNotEmpty() || _uiState.value.isLoading) return
        loadWeapons()
    }

    private fun loadWeapons() {
        viewModelScope.launch {
            _uiState.value = WeaponsUiState(isLoading = true)
            when (val result = repository.getWeapons()) {
                is Result.Success -> {
                    _uiState.value = WeaponsUiState(
                        weapons = result.data,
                        filteredWeapons = result.data
                    )
                }
                is Result.Error -> {
                    _uiState.value = WeaponsUiState(error = result.message)
                }
                else -> Unit
            }
        }
    }

    fun filterByCategory(category: String?) {
        val weapons = _uiState.value.weapons
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            filteredWeapons = if (category == null) weapons
            else weapons.filter { it.category.contains(category, ignoreCase = true) }
        )
    }
}