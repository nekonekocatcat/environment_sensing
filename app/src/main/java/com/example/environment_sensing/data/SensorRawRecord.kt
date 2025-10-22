//10秒ごとの生データのEntity（CSV保存に完全移行するなら不要）
package com.example.environment_sensing.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.environment_sensing.SensorData

@Entity
data class SensorRawRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val temperature: Double,
    val humidity: Double,
    val light: Int,
    val pressure: Double,
    val noise: Double,
    val tvoc: Int,
    val co2: Int,

    val latitude: Double? = null,
    val longitude: Double? = null
)

fun SensorRawRecord.toSensorData(): SensorData {
    return SensorData(
        temperature = this.temperature,
        humidity = this.humidity,
        light = this.light,
        pressure = this.pressure,
        noise = this.noise,
        tvoc = this.tvoc,
        co2 = this.co2
    )
}