package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class PlayerCardResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<PlayerCardData>?
)

data class PlayerCardData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("largeArt") val largeArt: String?,
    @SerializedName("wideArt") val wideArt: String?,
    @SerializedName("smallArt") val smallArt: String?
)