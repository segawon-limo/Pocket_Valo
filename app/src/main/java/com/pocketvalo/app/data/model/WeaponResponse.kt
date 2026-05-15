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
    @SerializedName("weaponStats") val weaponStats: WeaponStats?
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