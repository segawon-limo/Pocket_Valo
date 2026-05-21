package com.pocketvalo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached daily store for a player.
 * Store resets once per day — no need to re-fetch until expired.
 *
 * skinOfferUuids: comma-separated list of 4 skin level[0] UUIDs from SingleItemOffers
 * skinPrices: comma-separated "uuid:price" pairs, e.g. "abc:1775,def:875"
 */
@Entity(tableName = "store_cache")
data class StoreEntity(
    @PrimaryKey
    val puuid: String,
    val skinOfferUuids: String,         // "uuid1,uuid2,uuid3,uuid4"
    val skinPrices: String = "",        // "uuid1:1775,uuid2:875,uuid3:1275,uuid4:2175"
    val offersExpiresAt: Long,          // epoch seconds from API
    val vpBalance: Int,
    val radBalance: Int,
    val cachedAt: Long = System.currentTimeMillis()
)