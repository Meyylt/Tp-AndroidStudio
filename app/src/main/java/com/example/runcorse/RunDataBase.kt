package com.example.runcorse

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val photo: String?,
    val timestamp: Long,
    val synced: Boolean = false
)

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    fun allRuns(): Flow<List<RunEntity>>

    @Query("SELECT * FROM runs WHERE synced = 0")
    suspend fun getUnsyncedRuns(): List<RunEntity>

    @Upsert
    suspend fun upsert(run: RunEntity): Long

    // Le ": Int" est crucial ici pour éviter le crash de compilation
    @Query("UPDATE runs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>): Int
}

@Database(entities = [RunEntity::class], version = 1, exportSchema = false)
abstract class RunDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
}