package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class MatchHistoryResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<MatchData>?
)

data class MatchData(
    @SerializedName("metadata") val metadata: MatchMetadata,
    @SerializedName("players") val players: MatchPlayers?
)

data class MatchMetadata(
    @SerializedName("matchid") val matchId: String,
    @SerializedName("map") val map: String,
    @SerializedName("game_length") val gameLength: Int,
    @SerializedName("game_start_patched") val gameStartPatched: String,
    @SerializedName("mode") val mode: String,
    @SerializedName("region") val region: String
)

data class MatchPlayers(
    @SerializedName("all_players") val allPlayers: List<PlayerMatch>?
)

data class PlayerMatch(
    @SerializedName("name") val name: String,
    @SerializedName("tag") val tag: String,
    @SerializedName("team") val team: String,
    @SerializedName("character") val character: String,
    @SerializedName("stats") val stats: PlayerStats?
)

data class PlayerStats(
    @SerializedName("kills") val kills: Int,
    @SerializedName("deaths") val deaths: Int,
    @SerializedName("assists") val assists: Int,
    @SerializedName("headshots") val headshots: Int,
    @SerializedName("bodyshots") val bodyshots: Int
)