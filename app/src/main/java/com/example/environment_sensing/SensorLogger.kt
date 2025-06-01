package com.example.environment_sensing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorLogger(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var lastSavedTime = 0L
    private var job: Job? = null

    fun log(data: SensorData) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSavedTime >= 10_000) {
            lastSavedTime = currentTime

            job = scope.launch(Dispatchers.IO) {
                val timestampString = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                    .format(Date(currentTime))

                val csvLine = listOf(
                    timestampString,
                    data.temperature.toString(),
                    data.humidity.toString(),
                    data.light.toString(),
                    data.pressure.toString(),
                    data.noise.toString(),
                    data.tvoc.toString(),
                    data.co2.toString()
                ).joinToString(",")

                val file = File(context.getExternalFilesDir(null), "sensor_raw_data.csv")
                val isNewFile = file.createNewFile()

                val writer = FileWriter(file, true)

                if (isNewFile) {
                    writer.appendLine("timestamp,temperature,humidity,light,pressure,noise,tvoc,co2")
                }

                writer.appendLine(csvLine)
                writer.flush()
                writer.close()

                Log.d("RawLogger", "CSVに保存: $csvLine")
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}