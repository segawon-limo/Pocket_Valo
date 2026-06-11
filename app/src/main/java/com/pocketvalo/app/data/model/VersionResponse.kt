package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class VersionResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data")   val data: VersionData?
)

data class VersionData(
    @SerializedName("riotClientVersion")    val riotClientVersion: String,
    @SerializedName("riotClientBuild")      val riotClientBuild: String?,
    @SerializedName("buildDate")            val buildDate: String?
)