package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SensorRawDao {
    @Insert
    suspend fun insert(record: SensorRawRecord)

    @Query("SELECT * FROM SensorRawRecord ORDER BY timestamp DESC")
    suspend fun getAll(): List<SensorRawRecord>
}