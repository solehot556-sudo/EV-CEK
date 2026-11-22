package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.ElectricMotorEntity
import com.example.data.model.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotorDao {

    @Query("SELECT * FROM electric_motors ORDER BY priceIdr ASC")
    fun observeAllMotors(): Flow<List<ElectricMotorEntity>>

    @Query("SELECT * FROM electric_motors WHERE id = :id LIMIT 1")
    suspend fun getMotorById(id: String): ElectricMotorEntity?

    @Query("SELECT id FROM electric_motors WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>

    @Query("SELECT id FROM electric_motors")
    suspend fun getAllIds(): List<String>

    @Query("SELECT COUNT(*) FROM electric_motors")
    suspend fun getMotorCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(motors: List<ElectricMotorEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(motors: List<ElectricMotorEntity>)

    @Query("UPDATE electric_motors SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Transaction
    suspend fun upsertPreservingFavorites(motors: List<ElectricMotorEntity>) {
        if (motors.isEmpty()) return
        val favSet = getFavoriteIds().toHashSet()
        val merged = motors.map { motor ->
            if (favSet.contains(motor.id)) motor.copy(isFavorite = true) else motor
        }
        insertAll(merged)
    }

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 25")
    fun observeSyncLogs(): Flow<List<SyncLogEntity>>

    @Insert
    suspend fun insertSyncLog(log: SyncLogEntity)
}
