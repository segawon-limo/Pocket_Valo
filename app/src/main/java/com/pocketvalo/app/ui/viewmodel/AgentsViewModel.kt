package com.pocketvalo.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.model.AgentData
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AgentsUiState(
    val isLoading: Boolean = false,
    val agents: List<AgentData> = emptyList(),
    val filteredAgents: List<AgentData> = emptyList(),
    val selectedRole: String? = null,
    val error: String? = null
)

class AgentsViewModel : ViewModel() {

    private val repository = AssetsRepository()

    private val _uiState = MutableStateFlow(AgentsUiState())
    val uiState: StateFlow<AgentsUiState> = _uiState

    init {
        loadAgentsIfNeeded()
    }

    fun loadAgentsIfNeeded() {
        if (_uiState.value.agents.isNotEmpty() || _uiState.value.isLoading) return
        loadAgents()
    }

    private fun loadAgents() {
        viewModelScope.launch {
            _uiState.value = AgentsUiState(isLoading = true)
            when (val result = repository.getAgents()) {
                is Result.Success -> {
                    _uiState.value = AgentsUiState(
                        agents = result.data,
                        filteredAgents = result.data
                    )
                }
                is Result.Error -> {
                    _uiState.value = AgentsUiState(error = result.message)
                }
                else -> Unit
            }
        }
    }

    fun filterByRole(role: String?) {
        val agents = _uiState.value.agents
        _uiState.value = _uiState.value.copy(
            selectedRole = role,
            filteredAgents = if (role == null) agents
            else agents.filter { it.role?.displayName == role }
        )
    }
}