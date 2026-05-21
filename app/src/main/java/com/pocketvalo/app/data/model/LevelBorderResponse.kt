package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class LevelBorderResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<LevelBorderData>?
)

data class LevelBorderData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("startingLevel") val startingLevel: Int,
    // Border image untuk player card portrait
    @SerializedName("smallPlayerCardAppearance") val smallPlayerCardAppearance: String?,
    // Icon angka level
    @SerializedName("levelNumberAppearance") val levelNumberAppearance: String?
)