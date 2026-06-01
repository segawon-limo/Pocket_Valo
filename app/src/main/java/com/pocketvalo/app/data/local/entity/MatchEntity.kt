package com.pocketvalo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Flattened match summary for the logged-in player.
 * We only store what HomeScreen needs — no need to persist full scoreboard.
 * Full scoreboard is fetched fresh when user opens MatchDetail.
 */
@Entity(tableName = "match_history")
data class MatchEntity(
    @PrimaryKey
    val matchId: String,
    val ownerRiotId: String,        // FK to accounts.riotId — whose match history this is
    val map: String,
    val mode: String,
    val gameStartPatched: String,
    val gameStartEpoch: Long = 0L,
    val gameLength: Int,
    val roundsPlayed: Int,

    // Player-specific data for this match
    val agentName: String,
    val agentPortraitUrl: String?,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val score: Int,
    val rankName: String?,

    // Team result
    val playerTeam: String,         // "Red" or "Blue"
    val redRoundsWon: Int,
    val blueRoundsWon: Int,
    val hasWon: Boolean,

    val cachedAt: Long,             // System.currentTimeMillis()
    val matchIndex: Int = 0         // index from API response, 0 = most recent
)