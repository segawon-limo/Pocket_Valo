package com.pocketvalo.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for Riot auth tokens.
 * Never stores username/password — only tokens obtained after OAuth login.
 *
 * Stored keys:
 *  - access_token  : short-lived RSO token (~1 hour)
 *  - id_token      : used for region detection
 *  - refresh_token : long-lived, used to renew access_token without re-login
 *  - entitlement   : required for store API calls
 *  - puuid         : player UUID
 *  - region        : e.g. "ap", "na", "eu"
 *  - username      : gameName#tagLine, for display only
 */
class TokenStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "riot_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var idToken: String?
        get() = prefs.getString(KEY_ID_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ID_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var entitlementToken: String?
        get() = prefs.getString(KEY_ENTITLEMENT, null)
        set(value) = prefs.edit().putString(KEY_ENTITLEMENT, value).apply()

    var puuid: String?
        get() = prefs.getString(KEY_PUUID, null)
        set(value) = prefs.edit().putString(KEY_PUUID, value).apply()

    var region: String?
        get() = prefs.getString(KEY_REGION, null)
        set(value) = prefs.edit().putString(KEY_REGION, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var accessTokenExpiresAt: Long
        get() = prefs.getLong(KEY_ACCESS_EXPIRES_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_ACCESS_EXPIRES_AT, value).apply()

    val isLoggedIn: Boolean
        get() = refreshToken != null && puuid != null

    val isAccessTokenValid: Boolean
        get() = accessToken != null && System.currentTimeMillis() < accessTokenExpiresAt - 60_000L // 1 min buffer

    fun saveTokens(
        accessToken: String,
        idToken: String,
        refreshToken: String,
        expiresInSeconds: Int
    ) {
        this.accessToken = accessToken
        this.idToken = idToken
        this.refreshToken = refreshToken
        this.accessTokenExpiresAt = System.currentTimeMillis() + (expiresInSeconds * 1000L)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ENTITLEMENT = "entitlement_token"
        private const val KEY_PUUID = "puuid"
        private const val KEY_REGION = "region"
        private const val KEY_USERNAME = "username"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
    }
}