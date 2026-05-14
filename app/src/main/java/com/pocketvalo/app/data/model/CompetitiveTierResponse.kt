package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class CompetitiveTierResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<CompetitiveTierData>?
)

data class CompetitiveTierData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("tiers") val tiers: List<TierData>?
)

data class TierData(
    @SerializedName("tier") val tier: Int,
    @SerializedName("tierName") val tierName: String,
    @SerializedName("division") val division: String,
    @SerializedName("divisionName") val divisionName: String,
    @SerializedName("color") val color: String,
    @SerializedName("backgroundColor") val backgroundColor: String,
    @SerializedName("smallIcon") val smallIcon: String?,
    @SerializedName("largeIcon") val largeIcon: String?,
    @SerializedName("rankTriangleDownIcon") val rankTriangleDownIcon: String?,
    @SerializedName("rankTriangleUpIcon") val rankTriangleUpIcon: String?
)