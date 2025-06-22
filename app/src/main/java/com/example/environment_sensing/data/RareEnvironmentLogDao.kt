package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RareEnvironmentLogDao {

    @Insert
    suspend fun insert(log: RareEnvironmentLog)

    @Query("SELECT * FROM RareEnvironmentLog ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<RareEnvironmentLog>>
}