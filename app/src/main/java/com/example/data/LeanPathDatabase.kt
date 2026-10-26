package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "leanpath_profiles")
data class ProfileStateEntity(
    @PrimaryKey val profileKey: String,
    val displayName: String,
    val isActive: Boolean,
    val stateJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProfileStateDao {
    @Query("SELECT * FROM leanpath_profiles ORDER BY updatedAt ASC")
    fun observeAllProfiles(): Flow<List<ProfileStateEntity>>

    @Query("SELECT * FROM leanpath_profiles WHERE profileKey = :key LIMIT 1")
    suspend fun getProfileByKey(key: String): ProfileStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: ProfileStateEntity)

    @Query("UPDATE leanpath_profiles SET isActive = CASE WHEN profileKey = :activeKey THEN 1 ELSE 0 END")
    suspend fun setActiveProfile(activeKey: String)

    @Query("DELETE FROM leanpath_profiles WHERE profileKey = :key")
    suspend fun deleteProfileByKey(key: String)
}

@Database(
    entities = [ProfileStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LeanPathDatabase : RoomDatabase() {
    abstract fun profileStateDao(): ProfileStateDao

    companion object {
        @Volatile
        private var INSTANCE: LeanPathDatabase? = null

        fun getInstance(context: Context): LeanPathDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeanPathDatabase::class.java,
                    "leanpath_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
