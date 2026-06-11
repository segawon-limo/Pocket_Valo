package com.pocketvalo.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Token storage yang support multiple accounts.
 * Setiap akun disimpan dengan prefix "acct_{puuid}_*".
 * "active_puuid" menentukan akun yang sedang aktif.
 *
 * Backward-compatible: TokenStorage lama (file "riot_tokens") tetap dipakai
 * sebagai active account saat pertama kali migrasi.
 */
class MultiAccountTokenStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = createPrefs(context)

    private fun createPrefs(ctx: android.content.Context) = try {
        EncryptedSharedPreferences.create(
            ctx,
            "riot_tokens_multi",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        android.util.Log.w("MultiAccountStorage", "Corrupt prefs, clearing: \${e.message}")
        ctx.getSharedPreferences("riot_tokens_multi", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        try { ctx.deleteSharedPreferences("riot_tokens_multi") } catch (_: Exception) {}
        EncryptedSharedPreferences.create(
            ctx,
            "riot_tokens_multi",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Active account ────────────────────────────────────────────────────────

    var activePuuid: String?
        get() = prefs.getString(KEY_ACTIVE_PUUID, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_PUUID, value).apply()

    // ── Per-account helpers ───────────────────────────────────────────────────

    private fun key(puuid: String, field: String) = "acct_${puuid}_$field"

    fun saveAccount(
        puuid: String,
        accessToken: String,
        idToken: String,
        refreshToken: String,
        entitlementToken: String,
        region: String,
        username: String,
        expiresInSeconds: Int
    ) {
        prefs.edit().apply {
            putString(key(puuid, "access_token"), accessToken)
            putString(key(puuid, "id_token"), idToken)
            putString(key(puuid, "refresh_token"), refreshToken)
            putString(key(puuid, "entitlement_token"), entitlementToken)
            putString(key(puuid, "region"), region)
            putString(key(puuid, "username"), username)
            putLong(key(puuid, "access_expires_at"),
                System.currentTimeMillis() + (expiresInSeconds * 1000L))
            // Track list of known puuids
            val existing = getKnownPuuids().toMutableSet()
            existing.add(puuid)
            putString(KEY_KNOWN_PUUIDS, existing.joinToString(","))
        }.apply()
    }

    fun getAccessToken(puuid: String) = prefs.getString(key(puuid, "access_token"), null)
    fun getIdToken(puuid: String) = prefs.getString(key(puuid, "id_token"), null)
    fun getRefreshToken(puuid: String) = prefs.getString(key(puuid, "refresh_token"), null)
    fun getEntitlementToken(puuid: String) = prefs.getString(key(puuid, "entitlement_token"), null)
    fun getRegion(puuid: String) = prefs.getString(key(puuid, "region"), null)
    fun getUsername(puuid: String) = prefs.getString(key(puuid, "username"), null)
    fun getAccessExpiresAt(puuid: String) = prefs.getLong(key(puuid, "access_expires_at"), 0L)

    fun updateTokens(
        puuid: String,
        accessToken: String,
        idToken: String?,
        refreshToken: String,
        entitlementToken: String?,
        expiresInSeconds: Int
    ) {
        prefs.edit().apply {
            putString(key(puuid, "access_token"), accessToken)
            if (idToken != null) putString(key(puuid, "id_token"), idToken)
            putString(key(puuid, "refresh_token"), refreshToken)
            if (entitlementToken != null) putString(key(puuid, "entitlement_token"), entitlementToken)
            putLong(key(puuid, "access_expires_at"),
                System.currentTimeMillis() + (expiresInSeconds * 1000L))
        }.apply()
    }

    fun isAccessTokenValid(puuid: String): Boolean {
        val token = getAccessToken(puuid) ?: return false
        return System.currentTimeMillis() < getAccessExpiresAt(puuid) - 60_000L
    }

    fun removeAccount(puuid: String) {
        val existing = getKnownPuuids().toMutableSet()
        existing.remove(puuid)
        prefs.edit().apply {
            listOf("access_token", "id_token", "refresh_token", "entitlement_token",
                "region", "username", "access_expires_at").forEach { field ->
                remove(key(puuid, field))
            }
            putString(KEY_KNOWN_PUUIDS, existing.joinToString(","))
            if (activePuuid == puuid) putString(KEY_ACTIVE_PUUID, existing.firstOrNull())
        }.apply()
    }

    fun getKnownPuuids(): List<String> {
        val raw = prefs.getString(KEY_KNOWN_PUUIDS, "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }
    }

    /** Clear semua data — dipanggil saat deteksi stale MIUI backup */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ── Active account convenience getters ────────────────────────────────────

    val activeAccessToken get() = activePuuid?.let { getAccessToken(it) }
    val activeRefreshToken get() = activePuuid?.let { getRefreshToken(it) }
    val activeEntitlementToken get() = activePuuid?.let { getEntitlementToken(it) }
    val activeRegion get() = activePuuid?.let { getRegion(it) }
    val activeUsername get() = activePuuid?.let { getUsername(it) }
    val isActiveAccessTokenValid get() = activePuuid?.let { isAccessTokenValid(it) } ?: false

    val isLoggedIn: Boolean
        get() = activePuuid != null && getKnownPuuids().isNotEmpty()

    companion object {
        private const val KEY_ACTIVE_PUUID  = "active_puuid"
        private const val KEY_KNOWN_PUUIDS  = "known_puuids"
    }
}