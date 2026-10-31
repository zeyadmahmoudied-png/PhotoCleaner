package com.photocleaner.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photo_items")
    fun getAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photo_items WHERE groupId = :groupId")
    suspend fun getByGroup(groupId: String): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PhotoEntity>)

    @Query("DELETE FROM photo_items")
    suspend fun clearAll()
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM duplicate_groups")
    fun getAll(): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GroupEntity>)

    @Query("DELETE FROM duplicate_groups")
    suspend fun clearAll()
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getAll(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrashEntity)

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM trash_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM trash_items")
    suspend fun count(): Int
}

@Database(
    entities = [PhotoEntity::class, GroupEntity::class, TrashEntity::class, ScanSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun groupDao(): GroupDao
    abstract fun trashDao(): TrashDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photo_cleaner.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
