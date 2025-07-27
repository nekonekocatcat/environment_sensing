package com.example.environment_sensing.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironmentCollectionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(collection: EnvironmentCollection)

    @Query("SELECT * FROM EnvironmentCollection")
    fun getAll(): Flow<List<EnvironmentCollection>>

    @Query("SELECT COUNT(*) FROM EnvironmentCollection WHERE name = :name")
    suspend fun countByName(name: String): Int
}