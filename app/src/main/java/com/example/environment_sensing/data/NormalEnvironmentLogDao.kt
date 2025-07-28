package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NormalEnvironmentLogDao {
    @Insert
    suspend fun insert(log: NormalEnvironmentLog)

    @Query("SELECT * FROM NormalEnvironmentLog ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<NormalEnvironmentLog>>
}