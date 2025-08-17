package com.example.environment_sensing

import kotlinx.coroutines.flow.MutableSharedFlow

object SensorEventBus {
    val sensorData = MutableSharedFlow<SensorData?>(replay = 1)
    val rareEvent = MutableSharedFlow<String>(replay = 0)
    val normalEvent = MutableSharedFlow<String>(replay = 0)
}