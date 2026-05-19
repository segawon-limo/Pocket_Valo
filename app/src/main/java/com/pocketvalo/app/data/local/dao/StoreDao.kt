package com.pocketvalo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketvalo.app.data.local.entity.StoreEntity

@Dao
interface StoreDao {

    @Query("SELECT * FROM store_cache WHERE puuid = :puuid LIMIT 1")
    suspend fun getStore(puuid: String): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStore(store: StoreEntity)

    @Query("DELETE FROM store_cache WHERE puuid = :puuid")
    suspend fun clearStore(puuid: String)

    @Query("DELETE FROM store_cache")
    suspend fun clearAll()
}