package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorRawDao {
    @Insert
    suspend fun insert(record: SensorRawRecord)

    @Query("SELECT * FROM SensorRawRecord ORDER BY timestamp DESC")
    suspend fun getAll(): List<SensorRawRecord>

    @Query("SELECT * FROM SensorRawRecord ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SensorRawRecord>>

    @Query("SELECT * FROM SensorRawRecord ORDER BY timestamp DESC LIMIT :count")
    suspend fun getRecentRecords(count: Int): List<SensorRawRecord>
}