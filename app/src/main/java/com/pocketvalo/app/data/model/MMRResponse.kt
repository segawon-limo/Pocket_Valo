package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class MMRResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data")   val data: MMRData?
)

data class MMRData(
    @SerializedName("current_data") val currentData: MMRCurrentData?,
    @SerializedName("highest_rank") val highestRank: MMRHighestRank?
)

data class MMRCurrentData(
    @SerializedName("currenttier")        val currentTier: Int?,
    @SerializedName("currenttierpatched") val currentTierPatched: String?,  // "Silver 3"
    @SerializedName("images")            val images: MMRImages?,
    @SerializedName("ranking_in_tier")   val rankingInTier: Int?,           // RR (0-99)
    @SerializedName("mmr_change_to_last_game") val mmrChange: Int?
)

data class MMRImages(
    @SerializedName("small")  val small: String?,
    @SerializedName("large")  val large: String?,
    @SerializedName("triangle_down") val triangleDown: String?,
    @SerializedName("triangle_up")   val triangleUp: String?
)

data class MMRHighestRank(
    @SerializedName("patched_tier") val patchedTier: String?,
    @SerializedName("tier")         val tier: Int?
)