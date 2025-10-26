package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NormalEnvironmentLogDao {
    @Insert
    suspend fun insert(item: NormalEnvironmentLog): Long

    @Query("SELECT * FROM NormalEnvironmentLog ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<NormalEnvironmentLog>>

    @Query("SELECT * FROM NormalEnvironmentLog ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NormalEnvironmentLog>>

    @Query("SELECT * FROM NormalEnvironmentLog WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp DESC")
    fun observeAllWithLocation(): Flow<List<NormalEnvironmentLog>>

    @Query("UPDATE NormalEnvironmentLog SET latitude = :lat, longitude = :lon WHERE id = :id")
    suspend fun updateLatLon(id: Int, lat: Double?, lon: Double?)

    @Query("SELECT COUNT(*) FROM NormalEnvironmentLog WHERE latitude IS NULL OR longitude IS NULL")
    suspend fun countMissingNormal(): Int

    @Query("SELECT * FROM NormalEnvironmentLog WHERE latitude IS NULL OR longitude IS NULL ORDER BY timestamp DESC LIMIT :limit")
    suspend fun findMissingNormal(limit: Int = 200): List<NormalEnvironmentLog>
}