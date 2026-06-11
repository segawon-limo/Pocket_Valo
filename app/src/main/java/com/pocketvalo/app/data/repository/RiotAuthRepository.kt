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
                android.util.Log.d(
                    "RiotAuth",
                    "Token response: accessToken=${tokenResp?.accessToken?.take(10)}, error=${tokenResp?.error}"
                )
                if (tokenResp == null) return@withContext AuthResult.Failure(Exception("Token exchange failed"))

                if (tokenResp.accessToken == null || tokenResp.refreshToken == null) {
                    return@withContext AuthResult.Failure(
                        Exception(
                            tokenResp.error ?: "No tokens in response"
                        )
                    )
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

                val puuid = userInfo.puuid
                    ?: return@withContext AuthResult.Failure(Exception("No puuid in userinfo"))
                val gameName = userInfo.account?.gameName ?: return@withContext AuthResult.Failure(
                    Exception("No gameName")
                )
                val tagLine = userInfo.account?.tagLine ?: return@withContext AuthResult.Failure(
                    Exception("No tagLine")
                )

                tokenStorage.puuid = puuid
                tokenStorage.username = "$gameName#$tagLine"

                // Save to MultiAccountTokenStorage — activePuuid TIDAK diset di sini
                multiStorage?.saveAccount(
                    puuid = puuid,
                    accessToken = tokenResp.accessToken,
                    idToken = tokenResp.idToken ?: "",
                    refreshToken = tokenResp.refreshToken,
                    entitlementToken = entitlement,
                    region = region,
                    username = "$gameName#$tagLine",
                    expiresInSeconds = tokenResp.expiresIn ?: 3600
                )

                android.util.Log.d(
                    "RiotAuth",
                    "Login complete: $gameName#$tagLine, region: $region"
                )
                AuthResult.Success(NewAccountData(puuid, gameName, tagLine, region))
            } catch (e: Exception) {
                android.util.Log.e(
                    "RiotAuth",
                    "loginWithCode exception: ${e::class.simpleName}: ${e.message}"
                )
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
                        puuid = puuid,
                        accessToken = tokenResp.accessToken,
                        idToken = tokenResp.idToken,
                        refreshToken = tokenResp.refreshToken ?: refreshToken,
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
                    puuid = puuid,
                    accessToken = tokenResp.accessToken,
                    idToken = tokenResp.idToken,
                    refreshToken = tokenResp.refreshToken ?: refreshToken,
                    entitlementToken = entitlement,
                    expiresInSeconds = tokenResp.expiresIn ?: 3600
                )

                // If this is the active account, sync to TokenStorage too
                if (ms.activePuuid == puuid) {
                    tokenStorage.saveTokens(
                        accessToken = tokenResp.accessToken,
                        idToken = tokenResp.idToken ?: tokenStorage.idToken ?: "",
                        refreshToken = tokenResp.refreshToken ?: refreshToken,
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
            android.util.Log.e(
                "RiotAuth",
                "Request failed (${request.url}): ${e::class.simpleName}: ${e.message}"
            )
            null
        }
    }

    fun logout() {
        tokenStorage.clearAll()
    }

    companion object {
        const val RIOT_USER_AGENT = "ShooterGame/13 Windows/10.0.19043.1.256.64bit"
    }

    // ── Native credential login ──────────────────────────────────────────────

    suspend fun loginWithCredentials(
        username: String,
        password: String
    ): AuthResult<NewAccountData> {
        return withContext(Dispatchers.IO) {
            try {
                // Cookie jar untuk maintain session antara requests
                val cookieStore = mutableListOf<okhttp3.Cookie>()
                val cookieJar = object : okhttp3.CookieJar {
                    override fun saveFromResponse(
                        url: okhttp3.HttpUrl,
                        cookies: List<okhttp3.Cookie>
                    ) {
                        cookieStore.removeAll { c -> cookies.any { it.name == c.name } }
                        cookieStore.addAll(cookies)
                    }

                    override fun loadForRequest(url: okhttp3.HttpUrl) = cookieStore.toList()
                }

                val authClient = OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", RIOT_USER_AGENT)
                                .header("Content-Type", "application/json")
                                .build()
                        )
                    }
                    .build()

                // Step 1: Init auth session
                val initPayload = org.json.JSONObject().apply {
                    put("client_id", "riot-client")
                    put("nonce", "1")
                    put("redirect_uri", "http://localhost/redirect")
                    put("response_type", "token id_token")
                    put("scope", "openid offline_access lol ban profile email phone account")
                }

                val initReq = Request.Builder()
                    .url("https://auth.riotgames.com/api/v1/authorization")
                    .post(initPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val initResp = authClient.newCall(initReq).execute()
                val initBodyStr = initResp.body?.string() ?: ""
                android.util.Log.d("RiotAuth", "Init auth: ${initResp.code} body=$initBodyStr")
                android.util.Log.d("RiotAuth", "Cookies after init: ${cookieStore.map { "${it.name}=${it.value.take(10)}" }}")

                if (!initResp.isSuccessful) {
                    return@withContext AuthResult.Failure(Exception("Auth init failed: ${initResp.code}"))
                }

                // Step 2: Submit credentials — pakai JSONObject untuk proper escaping
                val credPayload = org.json.JSONObject().apply {
                    put("language", "en_US")
                    put("password", password)
                    put("remember", false)
                    put("type", "auth")
                    put("username", username)
                }
                android.util.Log.d("RiotAuth", "Sending credentials: username='$username' password_len=${password.length} password_bytes=${password.toByteArray(Charsets.UTF_8).take(4).map { it.toInt() }}")
                android.util.Log.d("RiotAuth", "Cred JSON: ${credPayload}")

                val credReq = Request.Builder()
                    .url("https://auth.riotgames.com/api/v1/authorization")
                    .put(credPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val credResp = authClient.newCall(credReq).execute()
                val credBodyStr = credResp.body?.string() ?: ""
                android.util.Log.d("RiotAuth", "Cred response ${credResp.code}: $credBodyStr")

                val respJson = try {
                    gson.fromJson(credBodyStr, com.google.gson.JsonObject::class.java)
                } catch (e: Exception) {
                    return@withContext AuthResult.Failure(Exception("Invalid response from Riot"))
                }
                val responseType = respJson.get("type")?.asString

                when (responseType) {
                    "multifactor" -> return@withContext AuthResult.Failure(Exception("MFA_REQUIRED"))
                    "auth" -> {
                        // type=auth tanpa "response" field = error
                        val errorCode = respJson.get("error")?.asString
                        return@withContext AuthResult.Failure(
                            Exception(
                                when (errorCode) {
                                    "auth_failure" -> "Wrong username or password"
                                    "rate_limited" -> "Too many attempts. Please wait."
                                    "invalid_session" -> "Session expired, please try again"
                                    else -> "Login failed: ${errorCode ?: "unknown"}"
                                }
                            )
                        )
                    }
                    null -> return@withContext AuthResult.Failure(Exception("Unexpected response: $credBodyStr"))
                    // "response" type = sukses, lanjut extract token
                }

                // Extract tokens dari URI fragment
                val uri = respJson.getAsJsonObject("response")
                    ?.getAsJsonObject("parameters")
                    ?.get("uri")?.asString
                    ?: return@withContext AuthResult.Failure(Exception("No redirect URI"))

                val fragment = android.net.Uri.parse(uri).fragment ?: ""
                val params = fragment.split("&").mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8")
                    else null
                }.toMap()

                val accessToken = params["access_token"]
                    ?: return@withContext AuthResult.Failure(Exception("No access_token"))
                val idToken = params["id_token"] ?: ""
                val expiresIn = params["expires_in"]?.toIntOrNull() ?: 3600

                tokenStorage.saveTokens(
                    accessToken = accessToken,
                    idToken = idToken,
                    refreshToken = "",
                    expiresInSeconds = expiresIn
                )

                val entitlement = fetchEntitlement()
                    ?: return@withContext AuthResult.Failure(Exception("Failed to fetch entitlement"))
                tokenStorage.entitlementToken = entitlement

                val region = fetchRegion(idToken)
                    ?: return@withContext AuthResult.Failure(Exception("Failed to fetch region"))
                tokenStorage.region = region

                val userInfo = fetchUserInfo()
                    ?: return@withContext AuthResult.Failure(Exception("Failed to fetch user info"))

                val puuid =
                    userInfo.puuid ?: return@withContext AuthResult.Failure(Exception("No puuid"))
                val gameName = userInfo.account?.gameName ?: return@withContext AuthResult.Failure(
                    Exception("No gameName")
                )
                val tagLine = userInfo.account?.tagLine ?: return@withContext AuthResult.Failure(
                    Exception("No tagLine")
                )

                tokenStorage.puuid = puuid
                tokenStorage.username = "$gameName#$tagLine"

                multiStorage?.saveAccount(
                    puuid = puuid,
                    accessToken = accessToken,
                    idToken = idToken,
                    refreshToken = "",
                    entitlementToken = entitlement,
                    region = region,
                    username = "$gameName#$tagLine",
                    expiresInSeconds = expiresIn
                )

                AuthResult.Success(NewAccountData(puuid, gameName, tagLine, region))
            } catch (e: Exception) {
                android.util.Log.e("RiotAuth", "loginWithCredentials error: ${e.message}")
                AuthResult.Failure(e)
            }
        }
    }
}