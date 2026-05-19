package com.pocketvalo.app.data.repository

import com.google.gson.Gson
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.dao.StoreDao
import com.pocketvalo.app.data.local.entity.StoreEntity
import com.pocketvalo.app.data.model.StorefrontResponse
import com.pocketvalo.app.data.model.WalletResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class StoreData(
    val skinUuids: List<String>,
    val offersExpiresAt: Long,
    val vpBalance: Int,
    val radBalance: Int,
    val fromCache: Boolean = false
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

    suspend fun getStore(forceRefresh: Boolean = false): AuthResult<StoreData> {
        val puuid = tokenStorage.puuid
            ?: return AuthResult.Failure(Exception("Not logged in"))

        // Return cache if valid
        if (!forceRefresh) {
            val cached = storeDao.getStore(puuid)
            if (cached != null && System.currentTimeMillis() / 1000 < cached.offersExpiresAt) {
                return AuthResult.Success(
                    StoreData(
                        skinUuids = cached.skinOfferUuids.split(","),
                        offersExpiresAt = cached.offersExpiresAt,
                        vpBalance = cached.vpBalance,
                        radBalance = cached.radBalance,
                        fromCache = true
                    )
                )
            }
        }

        // Ensure token valid
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

                if (storefrontResp.httpStatus == 400) {
                    return@withContext AuthResult.Failure(
                        Exception("Auth error: ${storefrontResp.errorCode}")
                    )
                }

                val skinOffers = storefrontResp.skinsPanelLayout?.singleItemOffers
                    ?: return@withContext AuthResult.Failure(Exception("No skin offers in response"))

                val remainingSec = storefrontResp.skinsPanelLayout.remainingDurationInSeconds
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
                        puuid = puuid,
                        skinOfferUuids = skinOffers.joinToString(","),
                        offersExpiresAt = expiresAt,
                        vpBalance = walletResp?.vp ?: 0,
                        radBalance = walletResp?.rad ?: 0
                    )
                )

                AuthResult.Success(
                    StoreData(
                        skinUuids = skinOffers,
                        offersExpiresAt = expiresAt,
                        vpBalance = walletResp?.vp ?: 0,
                        radBalance = walletResp?.rad ?: 0,
                        fromCache = false
                    )
                )
            } catch (e: Exception) {
                AuthResult.Failure(e)
            }
        }
    }

    private inline fun <reified T> execute(request: Request): T? {
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            android.util.Log.d("StoreRepository", "${request.url} -> ${response.code}: ${body.take(200)}")
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