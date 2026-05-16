package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

data class MatchDetailResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("data") val data: MatchDetailData?
)

data class MatchDetailData(
    @SerializedName("metadata") val metadata: MatchMetadata,
    @SerializedName("players") val players: MatchPlayers?,
    @SerializedName("teams") val teams: MatchTeams?,
    @SerializedName("rounds") val rounds: List<RoundData>?
)

data class RoundData(
    @SerializedName("winning_team") val winningTeam: String,       // "Red" or "Blue"
    @SerializedName("end_type") val endType: String,               // "Eliminated", "Bomb detonated", "Bomb defused", "Surrendered"
    @SerializedName("bomb_planted") val bombPlanted: Boolean,
    @SerializedName("bomb_defused") val bombDefused: Boolean,
    @SerializedName("plant_events") val plantEvents: PlantEvents?,
    @SerializedName("defuse_events") val defuseEvents: DefuseEvents?,
    @SerializedName("player_stats") val playerStats: List<RoundPlayerStats>?
)

data class PlantEvents(
    @SerializedName("plant_location") val plantLocation: Location?,
    @SerializedName("planted_by") val plantedBy: RoundActor?,
    @SerializedName("site") val site: String?                      // "A", "B", "C"
)

data class DefuseEvents(
    @SerializedName("defuse_location") val defuseLocation: Location?,
    @SerializedName("defused_by") val defusedBy: RoundActor?
)

data class Location(
    @SerializedName("x") val x: Int,
    @SerializedName("y") val y: Int
)

data class RoundActor(
    @SerializedName("puuid") val puuid: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("team") val team: String
)

data class RoundPlayerStats(
    @SerializedName("ability_casts") val abilityCasts: AbilityCasts?,
    @SerializedName("player_puuid") val puuid: String,
    @SerializedName("player_display_name") val displayName: String,
    @SerializedName("player_team") val team: String,
    @SerializedName("damage_events") val damageEvents: List<DamageEvent>?,
    @SerializedName("damage") val totalDamage: Int,
    @SerializedName("headshots") val headshots: Int,
    @SerializedName("bodyshots") val bodyshots: Int,
    @SerializedName("legshots") val legshots: Int,
    @SerializedName("kill_events") val killEvents: List<KillEvent>?,
    @SerializedName("kills") val kills: Int,
    @SerializedName("score") val score: Int,
    @SerializedName("economy") val economy: RoundEconomy?,
    @SerializedName("was_afk") val wasAfk: Boolean,
    @SerializedName("was_penalized") val wasPenalized: Boolean,
    @SerializedName("stayed_in_spawn") val stayedInSpawn: Boolean
)

data class AbilityCasts(
    @SerializedName("grenade_effects") val grenadeCasts: Int?,
    @SerializedName("ability1_effects") val ability1Casts: Int?,
    @SerializedName("ability2_effects") val ability2Casts: Int?,
    @SerializedName("ultimate_effects") val ultimateCasts: Int?
)

data class DamageEvent(
    @SerializedName("receiver_puuid") val receiverPuuid: String,
    @SerializedName("receiver_display_name") val receiverName: String,
    @SerializedName("receiver_team") val receiverTeam: String,
    @SerializedName("bodyshots") val bodyshots: Int,
    @SerializedName("damage") val damage: Int,
    @SerializedName("headshots") val headshots: Int,
    @SerializedName("legshots") val legshots: Int
)

data class KillEvent(
    @SerializedName("kill_time_in_round") val killTimeInRound: Int,   // ms
    @SerializedName("kill_time_in_match") val killTimeInMatch: Int,
    @SerializedName("killer_puuid") val killerPuuid: String,
    @SerializedName("killer_display_name") val killerName: String,
    @SerializedName("killer_team") val killerTeam: String,
    @SerializedName("victim_puuid") val victimPuuid: String,
    @SerializedName("victim_display_name") val victimName: String,
    @SerializedName("victim_team") val victimTeam: String,
    @SerializedName("victim_death_location") val deathLocation: Location?,
    @SerializedName("damage_weapon_id") val weaponId: String?,
    @SerializedName("damage_weapon_name") val weaponName: String?,
    @SerializedName("secondary_fire_mode") val secondaryFireMode: Boolean,
    @SerializedName("player_locations_on_kill") val playerLocations: List<PlayerLocation>?,
    @SerializedName("assistants") val assistants: List<RoundActor>?
)

data class PlayerLocation(
    @SerializedName("player_puuid") val puuid: String,
    @SerializedName("player_display_name") val displayName: String,
    @SerializedName("player_team") val team: String,
    @SerializedName("location") val location: Location?,
    @SerializedName("view_radians") val viewRadians: Double
)

data class RoundEconomy(
    @SerializedName("loadout_value") val loadoutValue: Int,
    @SerializedName("weapon") val weapon: EconomyWeapon?,
    @SerializedName("armor") val armor: EconomyArmor?,
    @SerializedName("remaining") val remaining: Int,
    @SerializedName("spent") val spent: Int
)

data class EconomyWeapon(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("assets_plain_video") val assetsPlainVideo: String?,
    @SerializedName("assets_display_icon") val displayIcon: String?
)

data class EconomyArmor(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,         // "Light Shields", "Heavy Shields"
    @SerializedName("assets_display_icon") val displayIcon: String?
)