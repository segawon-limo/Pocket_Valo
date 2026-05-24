package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

// ── Riot PVP API — /personalization/v2/players/{puuid}/playerloadout ──────────
data class PlayerLoadoutResponse(
    @SerializedName("Identity") val identity: PlayerIdentity?,
    @SerializedName("Guns") val guns: List<GunLoadout>?
)

data class GunLoadout(
    @SerializedName("ID") val weaponId: String,       // weapon UUID (matches WeaponData.uuid)
    @SerializedName("SkinID") val skinId: String       // equipped skin UUID (matches SkinData.uuid)
)

data class PlayerIdentity(
    @SerializedName("PlayerTitleID") val playerTitleId: String?,
    @SerializedName("PlayerCardID") val playerCardId: String?,
    @SerializedName("AccountLevel") val accountLevel: Int?
)

// ── valorant-api.com — /v1/playertitles/{uuid} ────────────────────────────────
data class PlayerTitleResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: PlayerTitleData?
)

data class PlayerTitleData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String?,
    // titleText adalah string yang ditampilkan — bisa null untuk default title
    @SerializedName("titleText") val titleText: String?,
    @SerializedName("isHiddenIfNotOwned") val isHiddenIfNotOwned: Boolean
)