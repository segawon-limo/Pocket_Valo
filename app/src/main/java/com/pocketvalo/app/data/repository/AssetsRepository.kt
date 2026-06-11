package com.pocketvalo.app.data.repository

import com.pocketvalo.app.data.model.LevelBorderData
import com.pocketvalo.app.data.model.TierData
import com.pocketvalo.app.data.model.MapData
import com.pocketvalo.app.data.model.AgentData
import com.pocketvalo.app.data.model.WeaponData
import com.pocketvalo.app.data.remote.api.RetrofitClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AssetsRepository private constructor() {

    private val api = RetrofitClient.valorantApi
    private var cachedTiers: Map<String, TierData> = emptyMap()
    private var cachedMaps: Map<String, MapData> = emptyMap()
    private var cachedAgents: List<AgentData> = emptyList()
    private var cachedWeapons: List<WeaponData> = emptyList()
    private var cachedLevelBorders: List<LevelBorderData> = emptyList()

    // Riot client version — di-fetch dari valorant-api.com/v1/version saat loading
    // Dipakai oleh StoreRepository untuk header X-Riot-ClientVersion
    @Volatile var clientVersion: String = ""

    // ── Singleton ─────────────────────────────────────────────────────────────

    // ── Bundle detail ─────────────────────────────────────────────────────────

    private val bundleCache   = mutableMapOf<String, com.pocketvalo.app.data.model.BundleDetail>()
    private val bundleFetched = mutableSetOf<String>() // track UUID yang sudah pernah di-fetch

    suspend fun getBundleDetail(uuid: String): com.pocketvalo.app.data.model.BundleDetail? {
        // Return cache kalau ada
        bundleCache[uuid]?.let { return it }
        // Jangan retry kalau sudah pernah fetch dan hasilnya null
        if (uuid in bundleFetched) return null

        bundleFetched.add(uuid)
        return try {
            android.util.Log.d("AssetsRepo", "Fetching bundle from API: $uuid")
            val resp = RetrofitClient.valorantApi.getBundle(uuid)
            android.util.Log.d("AssetsRepo", "Bundle response: ${resp.code()} body=${resp.body()?.data?.displayName}")
            val detail = resp.body()?.data
            if (detail != null) bundleCache[uuid] = detail
            detail
        } catch (e: Exception) {
            android.util.Log.e("AssetsRepo", "Bundle fetch error: ${e.message}")
            null
        }
    }

    companion object {
        @Volatile private var INSTANCE: AssetsRepository? = null

        fun getInstance(): AssetsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AssetsRepository().also { INSTANCE = it }
            }
    }

    // ── Competitive tiers ─────────────────────────────────────────────────────

    suspend fun getCompetitiveTiers(): Result<Map<String, TierData>> {
        if (cachedTiers.isNotEmpty()) return Result.Success(cachedTiers)
        return try {
            val response = api.getCompetitiveTiers()
            if (response.isSuccessful) {
                val latestEpisode = response.body()?.data?.lastOrNull()
                val tierMap = latestEpisode?.tiers
                    ?.filter { it.smallIcon != null }
                    ?.associateBy { it.tierName.uppercase() }
                    ?: emptyMap()
                cachedTiers = tierMap
                Result.Success(tierMap)
            } else Result.Error("Failed to load rank data")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // ── Maps ──────────────────────────────────────────────────────────────────

    suspend fun getMaps(): Result<Map<String, MapData>> {
        if (cachedMaps.isNotEmpty()) return Result.Success(cachedMaps)
        return try {
            val response = api.getMaps()
            if (response.isSuccessful) {
                val mapData = response.body()?.data
                    ?.associateBy { it.displayName.uppercase() }
                    ?: emptyMap()
                cachedMaps = mapData
                Result.Success(cachedMaps)
            } else Result.Error("Failed to load map data")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // ── Agents ────────────────────────────────────────────────────────────────

    suspend fun getAgents(): Result<List<AgentData>> {
        if (cachedAgents.isNotEmpty()) return Result.Success(cachedAgents)
        return try {
            val response = api.getAgents(isPlayableCharacter = true)
            if (response.isSuccessful) {
                val agents = response.body()?.data
                    ?.filter { it.isPlayableCharacter }
                    ?.sortedBy { it.displayName }
                    ?: emptyList()
                cachedAgents = agents
                Result.Success(agents)
            } else Result.Error("Failed to load agents")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // ── Level borders ─────────────────────────────────────────────────────────

    suspend fun getLevelBorderForLevel(accountLevel: Int): LevelBorderData? {
        if (cachedLevelBorders.isEmpty()) {
            try {
                val response = api.getLevelBorders()
                if (response.isSuccessful) {
                    cachedLevelBorders = response.body()?.data
                        ?.sortedBy { it.startingLevel }
                        ?: emptyList()
                }
            } catch (_: Exception) {
                return null
            }
        }
        return cachedLevelBorders
            .filter { it.startingLevel <= accountLevel }
            .maxByOrNull { it.startingLevel }
    }

    // ── Skin level cache ──────────────────────────────────────────────────────

    private val skinCacheMutex = Mutex()
    private var skinLevelCache: Map<String, StoreSkinInfo> = emptyMap()

    data class StoreSkinInfo(
        val displayName: String,
        val tierUuid: String?,
        val displayIcon: String?,
        val cost: Int,
        val videoUrl: String?
    )

    suspend fun getSkinByLevelUuid(levelUuid: String): StoreSkinInfo? {
        if (skinLevelCache.isEmpty()) {
            skinCacheMutex.withLock {
                // Double-check after acquiring lock — another coroutine may have populated it
                if (skinLevelCache.isEmpty()) {
                    buildSkinLevelCache()
                }
            }
        }
        return skinLevelCache[levelUuid]
    }

    /** Builds the full skin cache from /v1/weapons. Already guarded by skinCacheMutex. */
    private suspend fun buildSkinLevelCache() {
        try {
            val response = RetrofitClient.valorantApi.getWeapons()
            if (!response.isSuccessful) return

            val cache = mutableMapOf<String, StoreSkinInfo>()
            response.body()?.data?.forEach { weapon ->
                weapon.skins.forEach { skin ->
                    val icon = skin.levels.firstOrNull { it.displayIcon != null }?.displayIcon
                        ?: skin.chromas.firstOrNull()?.fullRender
                    val videoUrl = skin.levels.lastOrNull { it.streamedVideo != null }?.streamedVideo
                    val info = StoreSkinInfo(
                        displayName = skin.displayName,
                        tierUuid    = skin.contentTierUuid,
                        displayIcon = icon,
                        cost        = weapon.shopData?.cost ?: 0,
                        videoUrl    = videoUrl
                    )

                    // Index ALL levels — Night Market uses any level UUID, not just level[0]
                    skin.levels.forEach { level ->
                        if (level.uuid.isNotBlank()) cache[level.uuid] = info
                    }
                    // Also index by skin UUID itself (fallback)
                    if (skin.uuid.isNotBlank()) cache[skin.uuid] = info
                }
            }
            skinLevelCache = cache
        } catch (_: Exception) { }
    }

    /** Called by LoadingViewModel to warm the skin cache upfront. */
    suspend fun prefetchWeapons(): Result<List<WeaponData>> = getWeapons()

    // ── Weapons ───────────────────────────────────────────────────────────────

    suspend fun fetchClientVersion(): Result<String> {
        return try {
            val response = RetrofitClient.valorantApi.getVersion()
            val version  = response.body()?.data?.riotClientVersion
            if (response.isSuccessful && version != null) {
                clientVersion = version
                android.util.Log.d("AssetsRepository", "Client version fetched: $version")
                Result.Success(version)
            } else {
                android.util.Log.w("AssetsRepository", "Version fetch failed, using cached: $clientVersion")
                Result.Success(clientVersion.ifEmpty { "unknown" })
            }
        } catch (e: Exception) {
            android.util.Log.e("AssetsRepository", "fetchClientVersion error: ${e.message}")
            Result.Error(e.message ?: "Failed to fetch client version")
        }
    }

    suspend fun getWeapons(): Result<List<WeaponData>> {
        if (cachedWeapons.isNotEmpty()) return Result.Success(cachedWeapons)

        val categoryOrder = mapOf(
            "Melee"   to 0, "Sidearm" to 1, "SMG"    to 2,
            "Shotgun" to 3, "Rifle"   to 4, "Sniper" to 5, "Heavy" to 6
        )

        return try {
            val response = api.getWeapons()
            if (response.isSuccessful) {
                val weapons = response.body()?.data
                    ?.sortedWith(
                        compareBy(
                            { categoryOrder[it.category.substringAfterLast("::")] ?: 99 },
                            { it.shopData?.cost ?: 0 },
                            { it.displayName }
                        )
                    )
                    ?: emptyList()
                cachedWeapons = weapons
                // Also populate skin cache if it's empty (avoid double network call later)
                if (skinLevelCache.isEmpty()) {
                    skinCacheMutex.withLock {
                        if (skinLevelCache.isEmpty()) buildSkinLevelCache()
                    }
                }
                Result.Success(weapons)
            } else Result.Error("Failed to load weapons")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}