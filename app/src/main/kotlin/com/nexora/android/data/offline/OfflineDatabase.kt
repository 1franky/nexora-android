package com.nexora.android.data.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CachedResponseEntity::class, PendingOperationEntity::class], version = 1, exportSchema = false)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun cachedResponseDao(): CachedResponseDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        fun build(context: Context): OfflineDatabase =
            Room.databaseBuilder(context.applicationContext, OfflineDatabase::class.java, "nexora-offline.db").build()
    }
}
