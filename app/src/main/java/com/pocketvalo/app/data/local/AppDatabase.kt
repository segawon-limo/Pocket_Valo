package com.pocketvalo.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pocketvalo.app.data.local.dao.AccountDao
import com.pocketvalo.app.data.local.dao.WatchlistDao
import com.pocketvalo.app.data.local.dao.MatchDao
import com.pocketvalo.app.data.local.dao.StoreDao
import com.pocketvalo.app.data.local.entity.AccountEntity
import com.pocketvalo.app.data.local.entity.WatchlistEntity
import com.pocketvalo.app.data.local.entity.MatchEntity
import com.pocketvalo.app.data.local.entity.StoreEntity

@Database(
    entities = [AccountEntity::class, MatchEntity::class, StoreEntity::class, WatchlistEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun matchDao(): MatchDao
    abstract fun storeDao(): StoreDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocket_valo.db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}