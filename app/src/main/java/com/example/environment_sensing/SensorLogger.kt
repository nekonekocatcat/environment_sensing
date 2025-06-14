package com.example.environment_sensing

import android.content.Context
import android.util.Log
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.SensorRawRecord
import com.example.environment_sensing.data.toSensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorLogger(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onRareDetected: ((String) -> Unit)? = null
) {

    private val file = File(context.getExternalFilesDir(null), "sensor_raw_data.csv")
    private val database = AppDatabase.getInstance(context)

    fun log(data: SensorData) {
        val timestamp = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val csvLine = listOf(
            formattedTime,
            data.temperature,
            data.humidity,
            data.light,
            data.pressure,
            data.noise,
            data.tvoc,
            data.co2
        ).joinToString(",")

        coroutineScope.launch {
            // 1. CSV保存
            val isNewFile = file.createNewFile()
            val writer = FileWriter(file, true)
            if (isNewFile) {
                writer.appendLine("timestamp,temperature,humidity,light,pressure,noise,tvoc,co2")
            }
            writer.appendLine(csvLine)
            writer.flush()
            writer.close()

            // 2. DB保存
            val record = SensorRawRecord(
                timestamp = timestamp,
                temperature = data.temperature,
                humidity = data.humidity,
                light = data.light,
                pressure = data.pressure,
                noise = data.noise,
                tvoc = data.tvoc,
                co2 = data.co2
            )
            database.sensorRawDao().insert(record)
            Log.d("SensorLogger", "データ保存 (CSV + DB): $record")

            // DBからレア環境の判定をしとります
            val latest = database.sensorRawDao().getLatest()
            val rareName = latest?.let { RareEnvironmentChecker.check(it.toSensorData()) }
            if (rareName != null) {
                onRareDetected?.invoke(rareName)
            }
        }
    }
}