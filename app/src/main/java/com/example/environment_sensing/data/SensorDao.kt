package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Insert
    suspend fun insert(record: SensorRecord)

    @Query("SELECT * FROM SensorRecord ORDER BY timestamp DESC")
    suspend fun getAll(): List<SensorRecord>

    @Query("SELECT * FROM sensorrecord ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<SensorRecord>>
}