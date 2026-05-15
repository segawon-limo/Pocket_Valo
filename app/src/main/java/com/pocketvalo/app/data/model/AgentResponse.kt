package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class AgentResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<AgentData>?
)

data class AgentData(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("description") val description: String,
    @SerializedName("developerName") val developerName: String,
    @SerializedName("role") val role: AgentRole?,
    @SerializedName("displayIcon") val displayIcon: String?,
    @SerializedName("fullPortrait") val fullPortrait: String?,
    @SerializedName("background") val background: String?,
    @SerializedName("backgroundGradientColors") val backgroundGradientColors: List<String>?,
    @SerializedName("isPlayableCharacter") val isPlayableCharacter: Boolean,
    @SerializedName("abilities") val abilities: List<AgentAbility>?
)

data class AgentRole(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("description") val description: String,
    @SerializedName("displayIcon") val displayIcon: String?
)

data class AgentAbility(
    @SerializedName("slot") val slot: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("description") val description: String,
    @SerializedName("displayIcon") val displayIcon: String?
)