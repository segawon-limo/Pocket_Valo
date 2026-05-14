package com.pocketvalo.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.model.AccountData
import com.pocketvalo.app.data.model.MatchData
import com.pocketvalo.app.data.repository.PlayerRepository
import com.pocketvalo.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.pocketvalo.app.BuildConfig
// Ganti dengan API key Henrik-3 kamu
//private const val API_KEY = "HDEV-your-api-key-here"
val API_KEY = BuildConfig.HENRIK_API_KEY

data class PlayerUiState(
    val isLoading: Boolean = false,
    val accountData: AccountData? = null,
    val matchHistory: List<MatchData> = emptyList(),
    val error: String? = null
)

class PlayerViewModel : ViewModel() {

    private val repository = PlayerRepository()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState

    fun loadPlayerData(name: String, tag: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState(isLoading = true)

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
                    _uiState.value = PlayerUiState(error = result.message)
                }
                is Result.Loading -> Unit
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
                is Result.Loading -> Unit
            }
        }
    }
}