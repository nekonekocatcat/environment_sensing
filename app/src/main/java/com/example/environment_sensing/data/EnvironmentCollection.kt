package com.example.environment_sensing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EnvironmentCollection(
    @PrimaryKey val environmentName: String,
    val name: String,
    val timestamp: Long
)