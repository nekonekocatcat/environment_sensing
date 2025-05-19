package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedSensorDao {
    @Insert
    suspend fun insert(record: ProcessedSensorRecord)

    @Query("SELECT * FROM ProcessedSensorRecord ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ProcessedSensorRecord>>

}