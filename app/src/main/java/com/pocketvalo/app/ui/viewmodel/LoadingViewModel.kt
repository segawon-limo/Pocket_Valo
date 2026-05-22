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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class LoadingUiState(
    val progress: Float = 0f,
    val stepLabel: String = "",
    val isDone: Boolean = false
)

class LoadingViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val authRepo     = RiotAuthRepository(tokenStorage)
    private val assetsRepo   = AssetsRepository.getInstance()
    private val storeRepo    = StoreRepository(
        tokenStorage   = tokenStorage,
        authRepository = authRepo,
        storeDao       = AppDatabase.getInstance(application).storeDao()
    )

    private val _uiState = MutableStateFlow(LoadingUiState())
    val uiState: StateFlow<LoadingUiState> = _uiState

    private val steps = listOf(
        "Loading maps...",
        "Loading agents...",
        "Loading weapons...",
        "Loading rank data...",
        "Loading your store...",
        "Loading your profile...",
        "Loading match history..."
    )

    fun startPrefetch(playerViewModel: PlayerViewModel) {
        viewModelScope.launch {
            withTimeoutOrNull(10_000L) {
                runPrefetch(playerViewModel)
            }
            _uiState.value = _uiState.value.copy(progress = 1f, stepLabel = "Ready!")
            kotlinx.coroutines.delay(300L)
            _uiState.value = _uiState.value.copy(isDone = true)
        }
    }

    private suspend fun runPrefetch(playerViewModel: PlayerViewModel) {
        val total = steps.size.toFloat()

        fun setStep(index: Int) {
            _uiState.value = _uiState.value.copy(
                progress  = index / total,
                stepLabel = steps.getOrElse(index) { "" }
            )
        }

        // Steps 0-3: public asset APIs — parallel
        setStep(0)
        val mapsDeferred    = viewModelScope.async { assetsRepo.getMaps() }
        val agentsDeferred  = viewModelScope.async { assetsRepo.getAgents() }
        val weaponsDeferred = viewModelScope.async { assetsRepo.getWeapons() }
        val tiersDeferred   = viewModelScope.async { assetsRepo.getCompetitiveTiers() }

        viewModelScope.launch {
            mapsDeferred.await()
            _uiState.value = _uiState.value.copy(progress = 1 / total, stepLabel = steps[1])
        }
        viewModelScope.launch {
            agentsDeferred.await()
            _uiState.value = _uiState.value.copy(progress = 2 / total, stepLabel = steps[2])
        }
        viewModelScope.launch {
            weaponsDeferred.await()
            _uiState.value = _uiState.value.copy(progress = 3 / total, stepLabel = steps[3])
        }

        mapsDeferred.await()
        agentsDeferred.await()
        weaponsDeferred.await()
        tiersDeferred.await()

        // Step 4: Store
        _uiState.value = _uiState.value.copy(progress = 4 / total, stepLabel = steps[4])
        authRepo.ensureValidToken()
        storeRepo.getStore(forceRefresh = false)

        // Step 5: Player loadout / title
        _uiState.value = _uiState.value.copy(progress = 5 / total, stepLabel = steps[5])
        storeRepo.fetchPlayerTitle()

        // Step 6: Match history — via shared PlayerViewModel
        // loadFromTokenIfNeeded() sudah ada guard: skip kalau data sudah ada
        _uiState.value = _uiState.value.copy(progress = 6 / total, stepLabel = steps[6])
        playerViewModel.loadFromTokenIfNeeded()

        // Tunggu sampai PlayerViewModel selesai loading (atau timeout outer sudah handle)
        var waited = 0
        while (playerViewModel.uiState.value.isLoading && waited < 5_000) {
            kotlinx.coroutines.delay(200L)
            waited += 200
        }

        _uiState.value = _uiState.value.copy(progress = 7 / total, stepLabel = "Ready!")
    }
}