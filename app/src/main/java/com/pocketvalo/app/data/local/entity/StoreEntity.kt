package com.pocketvalo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_cache")
data class StoreEntity(
    @PrimaryKey
    val puuid: String,
    val skinOfferUuids: String,         // "uuid1,uuid2,uuid3,uuid4"
    val skinPrices: String = "",        // "uuid1:1775,uuid2:875"
    val offersExpiresAt: Long,
    val vpBalance: Int,
    val radBalance: Int,
    val cachedAt: Long = System.currentTimeMillis(),
    // Night market — JSON string: "[{uuid,original,discounted,pct},...]", "" kalau tidak ada
    val nightMarketJson: String = "",
    val nightMarketRemainingSeconds: Long = 0L
)