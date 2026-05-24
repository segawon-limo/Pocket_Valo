package com.pocketvalo.app.data.repository

import com.google.gson.Gson
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.dao.StoreDao
import com.pocketvalo.app.data.local.entity.StoreEntity
import com.pocketvalo.app.data.model.PlayerLoadoutResponse
import com.pocketvalo.app.data.model.StorefrontResponse
import com.pocketvalo.app.data.model.WalletResponse
import com.pocketvalo.app.data.remote.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class StoreData(
    val skinUuids: List<String>,
    val skinPrices: Map<String, Int> = emptyMap(),
    val offersExpiresAt: Long,
    val vpBalance: Int,
    val radBalance: Int,
    val fromCache: Boolean = false
)

data class PlayerTitleInfo(
    val titleText: String?,     // null = no title equipped
    val titleUuid: String?,
    val playerCardId: String?,  // UUID dari loadout → construct largeart URL
    val accountLevel: Int?      // dari loadout Identity
)

class StoreRepository(
    private val tokenStorage: TokenStorage,
    private val authRepository: RiotAuthRepository,
    private val storeDao: StoreDao
) {
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", RiotAuthRepository.RIOT_USER_AGENT)
                    .header("X-Riot-ClientPlatform", RIOT_CLIENT_PLATFORM)
                    .header("X-Riot-ClientVersion", RIOT_CLIENT_VERSION)
                    .build()
            )
        }
        .build()

    // ── Store ─────────────────────────────────────────────────────────────────

    suspend fun getStore(forceRefresh: Boolean = false): AuthResult<StoreData> {
        val puuid = tokenStorage.puuid
            ?: return AuthResult.Failure(Exception("Not logged in"))

        if (!forceRefresh) {
            val cached = storeDao.getStore(puuid)
            if (cached != null && System.currentTimeMillis() / 1000 < cached.offersExpiresAt) {
                val prices = parsePricesFromCache(cached.skinPrices)
                return AuthResult.Success(
                    StoreData(
                        skinUuids      = cached.skinOfferUuids.split(","),
                        skinPrices     = prices,
                        offersExpiresAt = cached.offersExpiresAt,
                        vpBalance      = cached.vpBalance,
                        radBalance     = cached.radBalance,
                        fromCache      = true
                    )
                )
            }
        }

        val tokenResult = authRepository.ensureValidToken()
        if (tokenResult.isFailure) {
            return AuthResult.Failure(
                tokenResult.exceptionOrNull() ?: Exception("Token refresh failed")
            )
        }

        val region = tokenStorage.region
            ?: return AuthResult.Failure(Exception("Region not set"))
        val accessToken = tokenStorage.accessToken
            ?: return AuthResult.Failure(Exception("No access token"))
        val entitlement = tokenStorage.entitlementToken
            ?: return AuthResult.Failure(Exception("No entitlement token"))

        return withContext(Dispatchers.IO) {
            try {
                val emptyJson = "{}".toRequestBody("application/json".toMediaType())

                val storefrontResp = execute<StorefrontResponse>(
                    Request.Builder()
                        .url("https://pd.$region.a.pvp.net/store/v3/storefront/$puuid")
                        .post(emptyJson)
                        .header("Authorization", "Bearer $accessToken")
                        .header("X-Riot-Entitlements-JWT", entitlement)
                        .build()
                ) ?: return@withContext AuthResult.Failure(Exception("Failed to fetch storefront"))
                android.util.Log.d("StoreDebug", "SkinsPanelLayout: ${gson.toJson(storefrontResp.skinsPanelLayout)}")

                if (storefrontResp.httpStatus == 400) {
                    return@withContext AuthResult.Failure(
                        Exception("Auth error: ${storefrontResp.errorCode}")
                    )
                }

                val skinsPanelLayout = storefrontResp.skinsPanelLayout
                    ?: return@withContext AuthResult.Failure(Exception("No skin offers in response"))

                val skinOffers = skinsPanelLayout.singleItemOffers

                val skinPrices: Map<String, Int> = skinsPanelLayout.singleItemStoreOffers
                    ?.associate { storeOffer ->
                        storeOffer.offerId to storeOffer.vpCost
                    }
                    ?: emptyMap()

                val remainingSec = skinsPanelLayout.remainingDurationInSeconds
                val expiresAt = System.currentTimeMillis() / 1000 + remainingSec

                val walletResp = execute<WalletResponse>(
                    Request.Builder()
                        .url("https://pd.$region.a.pvp.net/store/v1/wallet/$puuid")
                        .get()
                        .header("Authorization", "Bearer $accessToken")
                        .header("X-Riot-Entitlements-JWT", entitlement)
                        .build()
                )

                storeDao.upsertStore(
                    StoreEntity(
                        puuid           = puuid,
                        skinOfferUuids  = skinOffers.joinToString(","),
                        skinPrices      = serializePrices(skinPrices),
                        offersExpiresAt = expiresAt,
                        vpBalance       = walletResp?.vp ?: 0,
                        radBalance      = walletResp?.rad ?: 0
                    )
                )

                AuthResult.Success(
                    StoreData(
                        skinUuids       = skinOffers,
                        skinPrices      = skinPrices,
                        offersExpiresAt = expiresAt,
                        vpBalance       = walletResp?.vp ?: 0,
                        radBalance      = walletResp?.rad ?: 0,
                        fromCache       = false
                    )
                )
            } catch (e: Exception) {
                AuthResult.Failure(e)
            }
        }
    }

    // ── Player title ──────────────────────────────────────────────────────────
    // Step 1: fetch loadout dari PVP API → dapat PlayerTitleID
    // Step 2: resolve UUID ke nama title via valorant-api.com

    suspend fun fetchPlayerTitle(): AuthResult<PlayerTitleInfo> {
        val tokenResult = authRepository.ensureValidToken()
        if (tokenResult.isFailure) {
            return AuthResult.Failure(
                tokenResult.exceptionOrNull() ?: Exception("Token refresh failed")
            )
        }

        val puuid       = tokenStorage.puuid       ?: return AuthResult.Failure(Exception("Not logged in"))
        val region      = tokenStorage.region      ?: return AuthResult.Failure(Exception("Region not set"))
        val accessToken = tokenStorage.accessToken ?: return AuthResult.Failure(Exception("No access token"))
        val entitlement = tokenStorage.entitlementToken ?: return AuthResult.Failure(Exception("No entitlement token"))

        return withContext(Dispatchers.IO) {
            try {
                // Step 1 — loadout dari PVP API
                val loadout = execute<PlayerLoadoutResponse>(
                    Request.Builder()
                        .url("https://pd.$region.a.pvp.net/personalization/v2/players/$puuid/playerloadout")
                        .get()
                        .header("Authorization", "Bearer $accessToken")
                        .header("X-Riot-Entitlements-JWT", entitlement)
                        .build()
                )

                val titleId     = loadout?.identity?.playerTitleId
                val playerCardId = loadout?.identity?.playerCardId
                val accountLevel = loadout?.identity?.accountLevel

                if (titleId == null) {
                    return@withContext AuthResult.Success(
                        PlayerTitleInfo(null, null, playerCardId, accountLevel)
                    )
                }

                // Default title UUID — player belum set title
                if (titleId == "00000000-0000-0000-0000-000000000000") {
                    return@withContext AuthResult.Success(
                        PlayerTitleInfo(null, titleId, playerCardId, accountLevel)
                    )
                }

                // Step 2 — resolve title UUID ke nama via valorant-api.com
                val titleResp = RetrofitClient.valorantApi.getPlayerTitle(titleId)
                val titleText = titleResp.body()?.data?.titleText

                AuthResult.Success(PlayerTitleInfo(titleText, titleId, playerCardId, accountLevel))
            } catch (e: Exception) {
                AuthResult.Failure(e)
            }
        }
    }

    // ── Equipped skins ────────────────────────────────────────────────────────
    // Returns Map<weaponUuid, equippedSkinUuid> dari loadout player

    suspend fun fetchEquippedSkins(): AuthResult<Map<String, String>> {
        val tokenResult = authRepository.ensureValidToken()
        if (tokenResult.isFailure) {
            return AuthResult.Failure(
                tokenResult.exceptionOrNull() ?: Exception("Token refresh failed")
            )
        }

        val puuid       = tokenStorage.puuid       ?: return AuthResult.Failure(Exception("Not logged in"))
        val region      = tokenStorage.region      ?: return AuthResult.Failure(Exception("Region not set"))
        val accessToken = tokenStorage.accessToken ?: return AuthResult.Failure(Exception("No access token"))
        val entitlement = tokenStorage.entitlementToken ?: return AuthResult.Failure(Exception("No entitlement token"))

        return withContext(Dispatchers.IO) {
            try {
                val loadout = execute<PlayerLoadoutResponse>(
                    Request.Builder()
                        .url("https://pd.$region.a.pvp.net/personalization/v2/players/$puuid/playerloadout")
                        .get()
                        .header("Authorization", "Bearer $accessToken")
                        .header("X-Riot-Entitlements-JWT", entitlement)
                        .build()
                )

                val skinMap = loadout?.guns
                    ?.associate { it.weaponId to it.skinId }
                    ?: emptyMap()

                AuthResult.Success(skinMap)
            } catch (e: Exception) {
                AuthResult.Failure(e)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parsePricesFromCache(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap()
    }

    private fun serializePrices(prices: Map<String, Int>): String {
        return prices.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    private inline fun <reified T> execute(request: Request): T? {
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            android.util.Log.d("StoreRepository", "${request.url} -> ${response.code}: ${body.take(2000)}")
            if (!response.isSuccessful) return null
            gson.fromJson(body, T::class.java)
        } catch (e: Exception) {
            android.util.Log.e("StoreRepository", "Request failed: ${e::class.simpleName}: ${e.message}")
            null
        }
    }

    companion object {
        private const val RIOT_CLIENT_PLATFORM =
            "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9"
        private const val RIOT_CLIENT_VERSION = "release-09.09-shipping-9-2459494"
    }
}