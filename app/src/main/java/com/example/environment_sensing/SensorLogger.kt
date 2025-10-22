package com.example.environment_sensing

import android.content.Context
import android.util.Log
import com.example.environment_sensing.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.util.*

class SensorLogger(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onRareDetected: ((String) -> Unit)? = null,
    private val onNormalDetected: ((String) -> Unit)? = null,
    private val locationProvider: LocationProvider = LocationProvider(context)
) {
    private val file = File(context.getExternalFilesDir(null), "sensor_raw_data.csv")
    private val database = AppDatabase.getInstance(context)

    fun log(data: SensorData) {
        val timestamp = System.currentTimeMillis()

        coroutineScope.launch(Dispatchers.IO) {
            // 現在位置を取得
            val geo = locationProvider.currentOrNull()
            val lat = geo?.lat
            val lon = geo?.lon

            // CSV保存
            val isNewFile = file.createNewFile()
            FileWriter(file, true).use { writer ->
                if (isNewFile) {
                    writer.appendLine("timestamp,temperature,humidity,light,pressure,noise,tvoc,co2,lat,lon")
                }
                writer.appendLine(listOf(timestamp, data.temperature, data.humidity, data.light,
                    data.pressure, data.noise, data.tvoc, data.co2, lat, lon).joinToString(","))
            }

            // DB保存（生データ）
            database.sensorRawDao().insert(
                SensorRawRecord(
                    timestamp = timestamp,
                    temperature = data.temperature,
                    humidity = data.humidity,
                    light = data.light,
                    pressure = data.pressure,
                    noise = data.noise,
                    tvoc = data.tvoc,
                    co2 = data.co2,
                    latitude = lat,
                    longitude = lon
                )
            )

            // レア環境
            val rareName = RareEnvironmentChecker.check(data)
            if (rareName != null) {
                database.rareEnvironmentLogDao().insert(
                    RareEnvironmentLog(
                        environmentName = rareName,
                        timestamp = timestamp,
                        latitude = lat,
                        longitude = lon
                    )
                )
                withContext(Dispatchers.Main) {
                    onRareDetected?.invoke(rareName)
                }
                return@launch
            }

            // ノーマル環境
            val normalName = NormalEnvironmentChecker.check(data)
            if (normalName != null) {
                database.normalEnvironmentLogDao().insert(
                    NormalEnvironmentLog(
                        environmentName = normalName,
                        timestamp = timestamp,
                        latitude = lat,
                        longitude = lon
                    )
                )
                withContext(Dispatchers.Main) {
                    onNormalDetected?.invoke(normalName)
                }
            }
        }
    }
}