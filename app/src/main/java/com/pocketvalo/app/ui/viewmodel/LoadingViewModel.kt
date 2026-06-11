package com.pocketvalo.app.ui.viewmodel

import android.app.Application
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoadingUiState(
    val progress: Float = 0f,
    val stepLabel: String = "",
    val isDone: Boolean = false,
    val error: String? = null
)

class LoadingViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val multiStorage = MultiAccountTokenStorage(application)
    private val authRepo     = RiotAuthRepository(tokenStorage, multiStorage)
    private val assetsRepo   = AssetsRepository.getInstance()
    private val storeRepo    = StoreRepository(
        tokenStorage   = tokenStorage,
        authRepository = authRepo,
        storeDao       = AppDatabase.getInstance(application).storeDao()
    )

    private val _uiState = MutableStateFlow(LoadingUiState())
    val uiState: StateFlow<LoadingUiState> = _uiState

    private val steps = listOf(
        "Loading maps...",           // 0
        "Loading agents...",         // 1
        "Loading weapons...",        // 2
        "Loading rank data...",      // 3
        "Loading your store...",     // 4
        "Loading your profile...",   // 5
        "Loading rank info...",      // 6
        "Loading match history..."   // 7
    )

    private val total = steps.size.toFloat()

    private fun setStep(index: Int, label: String? = null) {
        _uiState.value = _uiState.value.copy(
            progress  = index / total,
            stepLabel = label ?: steps.getOrElse(index) { "" }
        )
    }

    fun startPrefetch(playerViewModel: PlayerViewModel, weaponsViewModel: WeaponsViewModel? = null) {
        viewModelScope.launch {
            try {
                runPrefetch(playerViewModel, weaponsViewModel)
                // Semua step selesai — done
                _uiState.value = _uiState.value.copy(
                    progress  = 1f,
                    stepLabel = "Ready!"
                )
                kotlinx.coroutines.delay(300L)
                _uiState.value = _uiState.value.copy(isDone = true)
            } catch (e: Exception) {
                android.util.Log.e("LoadingViewModel", "Prefetch error: ${e.message}")
                // Tetap lanjut ke app meski ada error — jangan stuck di loading
                _uiState.value = _uiState.value.copy(
                    progress  = 1f,
                    stepLabel = "Ready!"
                )
                kotlinx.coroutines.delay(300L)
                _uiState.value = _uiState.value.copy(isDone = true)
            }
        }
    }

    private fun prefetchAgentImages(agents: List<com.pocketvalo.app.data.model.AgentData>) {
        val imageLoader = ImageLoader(getApplication())
        agents.forEach { agent ->
            // Prefetch portrait utama
            agent.displayIcon?.let { url ->
                val request = ImageRequest.Builder(getApplication())
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .build()
                imageLoader.enqueue(request)
            }
            // Prefetch full portrait (dipakai di AgentDetailScreen)
            agent.fullPortrait?.let { url ->
                val request = ImageRequest.Builder(getApplication())
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .build()
                imageLoader.enqueue(request)
            }
            // Prefetch role icon
            agent.role?.displayIcon?.let { url ->
                val request = ImageRequest.Builder(getApplication())
                    .data(url)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    private suspend fun runPrefetch(playerViewModel: PlayerViewModel, weaponsViewModel: WeaponsViewModel? = null) {
        // ── Steps 0-3: public assets — jalankan parallel, tunggu semua selesai ──
        setStep(0)
        val mapsDeferred    = viewModelScope.async { assetsRepo.getMaps() }
        val agentsDeferred  = viewModelScope.async { assetsRepo.getAgents() }
        val weaponsDeferred = viewModelScope.async { assetsRepo.getWeapons() }
        val tiersDeferred   = viewModelScope.async { assetsRepo.getCompetitiveTiers() }
        val versionDeferred = viewModelScope.async { assetsRepo.fetchClientVersion() }

        // Update label seiring masing-masing selesai
        viewModelScope.launch { mapsDeferred.await();    setStep(1) }
        viewModelScope.launch {
            val agentsResult = agentsDeferred.await()
            setStep(2)
            // Prefetch gambar agent di background setelah data selesai
            // Ini populate Coil disk cache sehingga AgentsScreen langsung tampil
            if (agentsResult is com.pocketvalo.app.data.repository.Result.Success) {
                prefetchAgentImages(agentsResult.data)
            }
        }
        viewModelScope.launch { weaponsDeferred.await(); setStep(3) }

        // Tunggu semua 4 selesai sebelum lanjut
        awaitAll(mapsDeferred, agentsDeferred, weaponsDeferred, tiersDeferred, versionDeferred)

        // ── Step 4: Store + Night Market + Bundle prefetch ───────────────────
        setStep(4)
        authRepo.ensureValidToken()
        val storeResult = storeRepo.getStore(forceRefresh = false)

        // Prefetch bundle details (background, weapon images) agar siap saat StoreScreen dibuka
        if (storeResult is com.pocketvalo.app.data.repository.AuthResult.Success) {
            val bundles = storeResult.data.bundles
            if (bundles.isNotEmpty()) {
                bundles.forEach { bundleInfo ->
                    viewModelScope.launch { assetsRepo.getBundleDetail(bundleInfo.uuid) }
                }
            }
        }

        // ── Step 5: Player title + card + equipped skins (single API call) ──────
        setStep(5)
        when (val loadout = storeRepo.fetchPlayerLoadout()) {
            is AuthResult.Success -> {
                // Pass equipped skins ke WeaponsViewModel kalau tersedia
                // — avoid double hit ke playerloadout endpoint
                weaponsViewModel?.setEquippedSkins(loadout.data.equippedSkins)
            }
            is AuthResult.Failure -> {
                android.util.Log.w("LoadingViewModel", "fetchPlayerLoadout failed: ${loadout.error.message}")
            }
        }

        // ── Step 6: MMR / rank ─────────────────────────────────────────────────
        setStep(6)
        playerViewModel.loadFromTokenIfNeeded()
        val account = playerViewModel.uiState.value.accountData
        if (account != null) {
            playerViewModel.loadMmr(account.region, account.name, account.tag)
        }

        // ── Step 7: Match history ──────────────────────────────────────────────
        setStep(7)
        playerViewModel.loadFromTokenIfNeeded()

        // Tunggu PlayerViewModel selesai loading — tidak ada timeout paksa
        // Polling setiap 300ms, max 30 detik (network lambat masih bisa selesai)
        var waited = 0
        while (playerViewModel.uiState.value.isLoading && waited < 30_000) {
            kotlinx.coroutines.delay(300L)
            waited += 300
        }
    }
}