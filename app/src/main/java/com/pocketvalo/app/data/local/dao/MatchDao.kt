package com.pocketvalo.app.data.local.dao

import androidx.room.*
import com.pocketvalo.app.data.local.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Query("SELECT * FROM match_history WHERE ownerRiotId = :riotId ORDER BY matchIndex ASC")
    fun getMatchHistory(riotId: String): Flow<List<MatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatches(matches: List<MatchEntity>)

    // Keep only the last N matches per account — prevents unbounded growth
    @Query("""
        DELETE FROM match_history 
        WHERE ownerRiotId = :riotId 
        AND matchId NOT IN (
            SELECT matchId FROM match_history 
            WHERE ownerRiotId = :riotId 
            ORDER BY cachedAt DESC 
            LIMIT :keep
        )
    """)
    suspend fun trimHistory(riotId: String, keep: Int = 20)

    @Query("DELETE FROM match_history WHERE ownerRiotId = :riotId")
    suspend fun clearMatchHistory(riotId: String)
}