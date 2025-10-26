package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RareEnvironmentLogDao {

    @Insert
    suspend fun insert(item: RareEnvironmentLog): Long

    @Query("SELECT * FROM RareEnvironmentLog ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<RareEnvironmentLog>>


    @Query("SELECT * FROM RareEnvironmentLog ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RareEnvironmentLog>>

    @Query("SELECT * FROM RareEnvironmentLog WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp DESC")
    fun observeAllWithLocation(): Flow<List<RareEnvironmentLog>>

    @Query("UPDATE RareEnvironmentLog SET latitude = :lat, longitude = :lon WHERE id = :id")
    suspend fun updateLatLon(id: Int, lat: Double?, lon: Double?)

    @Query("SELECT COUNT(*) FROM RareEnvironmentLog WHERE latitude IS NULL OR longitude IS NULL")
    suspend fun countMissingRare(): Int

    @Query("SELECT * FROM RareEnvironmentLog WHERE latitude IS NULL OR longitude IS NULL ORDER BY timestamp DESC LIMIT :limit")
    suspend fun findMissingRare(limit: Int = 200): List<RareEnvironmentLog>
}