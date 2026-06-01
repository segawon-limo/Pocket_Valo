package com.pocketvalo.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStorage(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = createPrefs()

    /**
     * EncryptedSharedPreferences bisa throw AEADBadTagException saat reinstall
     * karena Android Keystore generate MasterKey baru tapi data lama masih
     * terenkripsi dengan key lama. Fix: hapus file lama dan recreate.
     */
    private fun createPrefs() = try {
        EncryptedSharedPreferences.create(
            context,
            "riot_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        android.util.Log.w("TokenStorage", "EncryptedSharedPreferences corrupt, clearing: ${e.message}")
        // Hapus file SharedPreferences yang corrupt
        context.getSharedPreferences("riot_tokens", Context.MODE_PRIVATE)
            .edit().clear().commit()
        try {
            context.deleteSharedPreferences("riot_tokens")
        } catch (_: Exception) {}
        // Hapus WebView cookies juga — supaya Riot tidak auto-login dengan session lama
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        } catch (_: Exception) {}
        // Recreate fresh
        EncryptedSharedPreferences.create(
            context,
            "riot_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

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
        get() = accessToken != null && System.currentTimeMillis() < accessTokenExpiresAt - 60_000L

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
        private const val KEY_ACCESS_TOKEN    = "access_token"
        private const val KEY_ID_TOKEN        = "id_token"
        private const val KEY_REFRESH_TOKEN   = "refresh_token"
        private const val KEY_ENTITLEMENT     = "entitlement_token"
        private const val KEY_PUUID           = "puuid"
        private const val KEY_REGION          = "region"
        private const val KEY_USERNAME        = "username"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
    }
}