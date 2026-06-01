package com.pocketvalo.app.data.local.dao

import androidx.room.*
import com.pocketvalo.app.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist WHERE puuid = :puuid ORDER BY addedAt DESC")
    fun getWatchlistForAccount(puuid: String): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE puuid = :puuid ORDER BY addedAt DESC")
    suspend fun getWatchlistForAccountOnce(puuid: String): List<WatchlistEntity>

    @Query("SELECT COUNT(*) FROM watchlist WHERE puuid = :puuid")
    suspend fun countForAccount(puuid: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE puuid = :puuid AND skinUuid = :skinUuid)")
    suspend fun isInWatchlist(puuid: String, skinUuid: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM watchlist WHERE puuid = :puuid AND skinUuid = :skinUuid")
    suspend fun deleteBySkin(puuid: String, skinUuid: String)
}