package com.example.environment_sensing

import kotlinx.coroutines.flow.MutableSharedFlow

object SensorEventBus {
    val sensorData = MutableSharedFlow<SensorData>(replay = 1, extraBufferCapacity = 1)

    val rareEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val normalEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val rareFirstEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val normalFirstEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
}