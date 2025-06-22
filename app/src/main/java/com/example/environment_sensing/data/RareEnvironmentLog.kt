package com.example.environment_sensing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RareEnvironmentLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val environmentName: String
)