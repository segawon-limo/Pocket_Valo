package com.pocketvalo.app.data.repository

import android.net.Uri
import com.google.gson.Gson
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
    private val tokenStorage: TokenStorage
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

    suspend fun loginWithCode(code: String): AuthResult<Unit> {
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
                    accessToken = tokenResp.accessToken,
                    idToken = tokenResp.idToken ?: "",
                    refreshToken = tokenResp.refreshToken,
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
                tokenStorage.puuid = userInfo.puuid
                tokenStorage.username = userInfo.account?.let { "${it.gameName}#${it.tagLine}" }

                android.util.Log.d("RiotAuth", "Login complete: ${tokenStorage.username}, region: ${tokenStorage.region}")
                AuthResult.Success(Unit)
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
            android.util.Log.d("RiotAuth", "${request.url} -> ${response.code}: ${body.take(200)}")
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