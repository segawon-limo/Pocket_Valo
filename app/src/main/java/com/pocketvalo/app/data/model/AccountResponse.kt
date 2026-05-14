package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class AccountResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: AccountData?
)

data class AccountData(
    @SerializedName("puuid") val puuid: String,
    @SerializedName("region") val region: String,
    @SerializedName("account_level") val accountLevel: Int,
    @SerializedName("name") val name: String,
    @SerializedName("tag") val tag: String,
    @SerializedName("card") val card: PlayerCard?
)

data class PlayerCard(
    @SerializedName("small") val small: String,
    @SerializedName("large") val large: String,
    @SerializedName("wide") val wide: String
)