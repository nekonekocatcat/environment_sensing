package com.example.environment_sensing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class NormalEnvironmentLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val environmentName: String,
    val timestamp: Long
)