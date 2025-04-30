package com.example.environment_sensing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SensorRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val temperature: Double,
    val humidity: Double,
    val light: Int,
    val pressure: Double,
    val noise: Double,
    val tvoc: Int,
    val co2: Int
)