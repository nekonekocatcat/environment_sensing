//使わない可能性大だけど一旦残しておく。今はMainActivity内でこの処理は行われている
package com.example.environment_sensing

import android.content.Context
import com.example.environment_sensing.data.SensorRawRecord
import java.io.File

class CsvReader(private val context: Context) {
    fun getLatestRecords(count: Int = 3): List<SensorRawRecord> {
        val file = File(context.getExternalFilesDir(null), "sensor_raw_data.csv")
        if (!file.exists()) return emptyList()

        val lines = file.readLines().drop(1).takeLast(count) // 先頭1行はヘッダーとして除外
        return lines.mapNotNull { line ->
            val tokens = line.split(",")
            try {
                SensorRawRecord(
                    timestamp = tokens[0].toLong(),
                    temperature = tokens[1].toDouble(),
                    humidity = tokens[2].toDouble(),
                    light = tokens[3].toInt(),
                    pressure = tokens[4].toDouble(),
                    noise = tokens[5].toDouble(),
                    tvoc = tokens[6].toInt(),
                    co2 = tokens[7].toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}