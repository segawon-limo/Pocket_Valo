package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class MatchHistoryResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: List<MatchData>?
)

data class MatchData(
    @SerializedName("metadata") val metadata: MatchMetadata,
    @SerializedName("players") val players: MatchPlayers?,
    @SerializedName("teams") val teams: MatchTeams?
)

data class MatchMetadata(
    @SerializedName("matchid") val matchId: String,
    @SerializedName("map") val map: String,
    @SerializedName("game_length") val gameLength: Int,
    @SerializedName("game_start_patched") val gameStartPatched: String,
    @SerializedName("mode") val mode: String,
    @SerializedName("region") val region: String,
    @SerializedName("rounds_played") val roundsPlayed: Int = 0
)

data class MatchPlayers(
    @SerializedName("all_players") val allPlayers: List<PlayerMatch>?
)

data class PlayerMatch(
    @SerializedName("name") val name: String,
    @SerializedName("tag") val tag: String,
    @SerializedName("team") val team: String,
    @SerializedName("character") val character: String,
    @SerializedName("currenttier_patched") val rankName: String? = null,
    @SerializedName("assets") val assets: PlayerAssets? = null,
    @SerializedName("stats") val stats: PlayerStats?
)

data class PlayerAssets(
    @SerializedName("agent") val agent: AgentAssets?
)

data class AgentAssets(
    @SerializedName("small") val small: String,
    @SerializedName("bust") val bust: String,
    @SerializedName("full") val full: String,
    @SerializedName("killfeed") val killfeed: String
)

data class PlayerStats(
    @SerializedName("kills") val kills: Int,
    @SerializedName("deaths") val deaths: Int,
    @SerializedName("assists") val assists: Int,
    @SerializedName("headshots") val headshots: Int,
    @SerializedName("bodyshots") val bodyshots: Int,
    @SerializedName("score") val score: Int = 0
)

data class MatchTeams(
    @SerializedName("red") val red: TeamData?,
    @SerializedName("blue") val blue: TeamData?
)

data class TeamData(
    @SerializedName("has_won") val hasWon: Boolean,
    @SerializedName("rounds_won") val roundsWon: Int,
    @SerializedName("rounds_lost") val roundsLost: Int
)