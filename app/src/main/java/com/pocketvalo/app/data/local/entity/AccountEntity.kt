package com.pocketvalo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val riotId: String,           // "segawon#limo" — unique identifier
    val gameName: String,
    val tagLine: String,
    val puuid: String,
    val region: String,
    val accountLevel: Int,
    val cardSmall: String?,       // player card image URL
    val lastSearched: Long        // System.currentTimeMillis()
)