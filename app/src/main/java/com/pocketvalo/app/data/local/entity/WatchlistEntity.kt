package com.pocketvalo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val id: String,             // "$puuid:$skinUuid" — unique per akun per skin
    val puuid: String,          // akun pemilik
    val skinUuid: String,       // UUID base skin (bukan level UUID)
    val levelUuid: String,      // level UUID untuk matching dengan store response
    val displayName: String,
    val iconUrl: String?,
    val weaponType: String?,    // "Rifle", "Pistol", dll
    val tierUuid: String?,
    val addedAt: Long = System.currentTimeMillis()
)