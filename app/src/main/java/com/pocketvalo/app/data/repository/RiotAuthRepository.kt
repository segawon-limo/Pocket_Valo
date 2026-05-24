package com.pocketvalo.app.data.repository

import android.net.Uri
import com.google.gson.Gson
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.model.EntitlementResponse
import com.pocketvalo.app.data.model.PasTokenResponse
import com.pocketvalo.app.data.model.RiotTokenResponse
import com.pocketvalo.app.data.model.RiotUserInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Failure(val error: Exception) : AuthResult<Nothing>()

    val isSuccess get() = this is Success
    val isFailure get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Exception? = (this as? Failure)?.error
}

class RiotAuthRepository(
    private val tokenStorage: TokenStorage,
    private val multiStorage: MultiAccountTokenStorage? = null
) {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", RIOT_USER_AGENT)
                    .build()
            )
        }
        .build()

    // ── Auth URL ──────────────────────────────────────────────────────────────

    fun generateAuthUrl(): String {
        val nonce = System.currentTimeMillis().toString(36)
        return "https://auth.riotgames.com/authorize?" +
                "client_id=riot-client" +
                "&redirect_uri=http://localhost/redirect" +
                "&response_type=code" +
                "&scope=openid link ban lol_region account offline_access" +
                "&nonce=$nonce"
    }

    // ── Full login flow after code obtained ───────────────────────────────────

    data class NewAccountData(
        val puuid: String,
        val gameName: String,
        val tagLine: String,
        val region: String
    )

    /**
     * Tukar auth code dengan token dan ambil info akun dari Riot.
     * TIDAK set activePuuid — caller yang memutuskan apakah ini login pertama
     * (set active) atau add-account (simpan saja, jangan switch).
     */
    suspend fun loginWithCode(code: String): AuthResult<NewAccountData> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("RiotAuth", "Exchanging code for tokens...")
                val tokenResp = exchangeCode(code)
                android.util.Log.d("RiotAuth", "Token response: accessToken=${tokenResp?.accessToken?.take(10)}, error=${tokenResp?.error}")
                if (tokenResp == null) return@withContext AuthResult.Failure(Exception("Token exchange failed"))

                if (tokenResp.accessToken == null || tokenResp.refreshToken == null) {
                    return@withContext AuthResult.Failure(Exception(tokenResp.error ?: "No tokens in response"))
                }

                tokenStorage.saveTokens(
                    accessToken      = tokenResp.accessToken,
                    idToken          = tokenResp.idToken ?: "",
                    refreshToken     = tokenResp.refreshToken,
                    expiresInSeconds = tokenResp.expiresIn ?: 3600
                )

                val entitlement = fetchEntitlement()
                    ?: return@withContext AuthResult.Failure(Exception("Failed to fetch entitlement"))
                tokenStorage.entitlementToken = entitlement

                val region = fetchRegion(tokenResp.idToken ?: "")
                    ?: return@withContext AuthResult.Failure(Exception("Failed to fetch region"))
                tokenStorage.region = region

                val userInfo = fetchUserInfo()
                    ?: return@withContext AuthResult.Failure(Exception("Failed to fetch user info"))

                val puuid    = userInfo.puuid    ?: return@withContext AuthResult.Failure(Exception("No puuid in userinfo"))
                val gameName = userInfo.account?.gameName ?: return@withContext AuthResult.Failure(Exception("No gameName"))
                val tagLine  = userInfo.account?.tagLine  ?: return@withContext AuthResult.Failure(Exception("No tagLine"))

                tokenStorage.puuid    = puuid
                tokenStorage.username = "$gameName#$tagLine"

                // Save to MultiAccountTokenStorage — activePuuid TIDAK diset di sini
                multiStorage?.saveAccount(
                    puuid            = puuid,
                    accessToken      = tokenResp.accessToken,
                    idToken          = tokenResp.idToken ?: "",
                    refreshToken     = tokenResp.refreshToken,
                    entitlementToken = entitlement,
                    region           = region,
                    username         = "$gameName#$tagLine",
                    expiresInSeconds = tokenResp.expiresIn ?: 3600
                )

                android.util.Log.d("RiotAuth", "Login complete: $gameName#$tagLine, region: $region")
                AuthResult.Success(NewAccountData(puuid, gameName, tagLine, region))
            } catch (e: Exception) {
                android.util.Log.e("RiotAuth", "loginWithCode exception: ${e::class.simpleName}: ${e.message}")
                AuthResult.Failure(e)
            }
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────────

    suspend fun ensureValidToken(): AuthResult<Unit> {
        if (tokenStorage.isAccessTokenValid) return AuthResult.Success(Unit)

        val refreshToken = tokenStorage.refreshToken
            ?: return AuthResult.Failure(Exception("No refresh token — user must re-login"))

        return withContext(Dispatchers.IO) {
            try {
                val tokenResp = refreshWithRefreshToken(refreshToken)
                    ?: return@withContext AuthResult.Failure(Exception("Token refresh failed"))

                if (tokenResp.accessToken == null) {
                    tokenStorage.clearAll()
                    return@withContext AuthResult.Failure(Exception("Refresh token revoked"))
                }

                tokenStorage.saveTokens(
                    accessToken = tokenResp.accessToken,
                    idToken = tokenResp.idToken ?: tokenStorage.idToken ?: "",
                    refreshToken = tokenResp.refreshToken ?: refreshToken,
                    expiresInSeconds = tokenResp.expiresIn ?: 3600
                )

                val entitlement = fetchEntitlement()
                if (entitlement != null) tokenStorage.entitlementToken = entitlement

                // Sync to MultiAccountTokenStorage
                multiStorage?.let { ms ->
                    val puuid = tokenStorage.puuid ?: return@let
                    ms.updateTokens(
                        puuid            = puuid,
                        accessToken      = tokenResp.accessToken,
                        idToken          = tokenResp.idToken,
                        refreshToken     = tokenResp.refreshToken ?: refreshToken,
                        entitlementToken = entitlement,
                        expiresInSeconds = tokenResp.expiresIn ?: 3600
                    )
                }

                AuthResult.Success(Unit)
            } catch (e: Exception) {
                AuthResult.Failure(e)
            }
        }
    }

    /**
     * Ensure token valid for a specific puuid (used by switch account flow).
     * Writes result back to TokenStorage (active session) and MultiAccountTokenStorage.
     */
    suspend fun ensureValidTokenForPuuid(puuid: String): AuthResult<Unit> {
        val ms = multiStorage ?: return AuthResult.Failure(Exception("MultiStorage not available"))

        if (ms.isAccessTokenValid(puuid)) return AuthResult.Success(Unit)

        val refreshToken = ms.getRefreshToken(puuid)
            ?: return AuthResult.Failure(Exception("No refresh token for $puuid"))

        return withContext(Dispatchers.IO) {
            try {
                val tokenResp = refreshWithRefreshToken(refreshToken)
                    ?: return@withContext AuthResult.Failure(Exception("Token refresh failed"))

                if (tokenResp.accessToken == null) {
                    ms.removeAccount(puuid)
                    return@withContext AuthResult.Failure(Exception("Refresh token revoked — re-login required"))
                }

                val entitlement = fetchEntitlementWithToken(tokenResp.accessToken)

                ms.updateTokens(
                    puuid            = puuid,
                    accessToken      = tokenResp.accessToken,
                    idToken          = tokenResp.idToken,
                    refreshToken     = tokenResp.refreshToken ?: refreshToken,
                    entitlementToken = entitlement,
                    expiresInSeconds = tokenResp.expiresIn ?: 3600
                )

                // If this is the active account, sync to TokenStorage too
                if (ms.activePuuid == puuid) {
                    tokenStorage.saveTokens(
                        accessToken      = tokenResp.accessToken,
                        idToken          = tokenResp.idToken ?: tokenStorage.idToken ?: "",
                        refreshToken     = tokenResp.refreshToken ?: refreshToken,
                        expiresInSeconds = tokenResp.expiresIn ?: 3600
                    )
                    if (entitlement != null) tokenStorage.entitlementToken = entitlement
                }

                AuthResult.Success(Unit)
            } catch (e: Exception) {
                AuthResult.Failure(e)
            }
        }
    }

    // ── Private API calls ─────────────────────────────────────────────────────

    private fun exchangeCode(code: String): RiotTokenResponse? {
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", "http://localhost/redirect")
            .add("client_id", "riot-client")
            .build()

        val request = Request.Builder()
            .url("https://auth.riotgames.com/token")
            .post(body)
            .build()

        return executeForJson(request)
    }

    private fun refreshWithRefreshToken(refreshToken: String): RiotTokenResponse? {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", "riot-client")
            .build()

        val request = Request.Builder()
            .url("https://auth.riotgames.com/token")
            .post(body)
            .build()

        return executeForJson(request)
    }

    private fun fetchEntitlement(): String? {
        val accessToken = tokenStorage.accessToken ?: return null
        return fetchEntitlementWithToken(accessToken)
    }

    private fun fetchEntitlementWithToken(accessToken: String): String? {
        val emptyBody = ByteArray(0).toRequestBody()
        val request = Request.Builder()
            .url("https://entitlements.auth.riotgames.com/api/token/v1")
            .post(emptyBody)
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json")
            .build()
        return executeForJson<EntitlementResponse>(request)?.entitlementsToken
    }

    private fun fetchRegion(idToken: String): String? {
        val accessToken = tokenStorage.accessToken ?: return null
        val jsonBody = """{"id_token":"$idToken"}"""
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://riot-geo.pas.si.riotgames.com/pas/v1/product/valorant")
            .put(jsonBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        return executeForJson<PasTokenResponse>(request)?.affinities?.live
    }

    private fun fetchUserInfo(): RiotUserInfoResponse? {
        val accessToken = tokenStorage.accessToken ?: return null

        val request = Request.Builder()
            .url("https://auth.riotgames.com/userinfo")
            .get()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return executeForJson(request)
    }

    private inline fun <reified T> executeForJson(request: Request): T? {
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            android.util.Log.d("RiotAuth", "${request.url} -> ${response.code}: ${body.take(5000)}")
            if (!response.isSuccessful) return null
            gson.fromJson(body, T::class.java)
        } catch (e: Exception) {
            android.util.Log.e("RiotAuth", "Request failed (${request.url}): ${e::class.simpleName}: ${e.message}")
            null
        }
    }

    fun logout() {
        tokenStorage.clearAll()
    }

    companion object {
        const val RIOT_USER_AGENT = "ShooterGame/13 Windows/10.0.19043.1.256.64bit"
    }
}