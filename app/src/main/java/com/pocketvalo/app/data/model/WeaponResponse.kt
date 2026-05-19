package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class WeaponResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<WeaponData>?
)

data class WeaponData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("category") val category: String,
    @SerializedName("displayIcon") val displayIcon: String?,
    @SerializedName("killStreamIcon") val killStreamIcon: String?,
    @SerializedName("weaponStats") val weaponStats: WeaponStats?,
    @SerializedName("shopData") val shopData: ShopData?,
    @SerializedName("skins") val skins: List<SkinData> = emptyList()
)

data class SkinData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("contentTierUuid") val contentTierUuid: String?,
    @SerializedName("displayIcon") val displayIcon: String?,
    @SerializedName("levels") val levels: List<SkinLevelData> = emptyList(),
    @SerializedName("chromas") val chromas: List<SkinChromaData> = emptyList()
)

data class SkinLevelData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("displayIcon") val displayIcon: String?
)

data class SkinChromaData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("fullRender") val fullRender: String?
)

data class ShopData(
    @SerializedName("cost") val cost: Int?,
    @SerializedName("category") val shopCategory: String?,
    @SerializedName("categoryText") val categoryText: String?
)

data class WeaponStats(
    @SerializedName("fireRate") val fireRate: Float?,
    @SerializedName("magazineSize") val magazineSize: Int?,
    @SerializedName("reloadTimeSeconds") val reloadTimeSeconds: Float?,
    @SerializedName("firstBulletAccuracy") val firstBulletAccuracy: Float?,
    @SerializedName("wallPenetration") val wallPenetration: String?,
    @SerializedName("damageRanges") val damageRanges: List<DamageRange>?
)

data class DamageRange(
    @SerializedName("rangeStartMeters") val rangeStartMeters: Int,
    @SerializedName("rangeEndMeters") val rangeEndMeters: Int,
    @SerializedName("headDamage") val headDamage: Float,
    @SerializedName("bodyDamage") val bodyDamage: Float,
    @SerializedName("legDamage") val legDamage: Float
)