package com.example.environment_sensing

import android.util.Log
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.SensorRawRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SensorLogger(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private var lastSavedTime = 0L
    private var job: Job? = null

    fun log(data: SensorData) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSavedTime >= 10_000) {
            lastSavedTime = currentTime

            job = scope.launch(Dispatchers.IO) {
                val record = SensorRawRecord(
                    timestamp = currentTime,
                    temperature = data.temperature,
                    humidity = data.humidity,
                    light = data.light,
                    pressure = data.pressure,
                    noise = data.noise,
                    tvoc = data.tvoc,
                    co2 = data.co2
                )
                database.sensorRawDao().insert(record)
                Log.d("RawLogger", "Rawデータを保存: $record")
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}