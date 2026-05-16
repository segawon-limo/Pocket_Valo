package com.pocketvalo.app.data.local.dao

import androidx.room.*
import com.pocketvalo.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY lastSearched DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE riotId = :riotId LIMIT 1")
    suspend fun getAccount(riotId: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE riotId = :riotId")
    suspend fun deleteAccount(riotId: String)
}