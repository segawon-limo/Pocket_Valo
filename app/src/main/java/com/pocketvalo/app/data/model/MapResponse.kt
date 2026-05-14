package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class MapResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<MapData>?
)

data class MapData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("splash") val splash: String?,
    @SerializedName("listViewIcon") val listViewIcon: String?,
    @SerializedName("listViewIconTall") val listViewIconTall: String?
)