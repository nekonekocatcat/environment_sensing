package com.example.environment_sensing

data class SensorData(
    val temperature: Double,
    val humidity: Double,
    val light: Int,
    val pressure: Double,
    val noise: Double,
    val tvoc: Int,
    val co2: Int
)