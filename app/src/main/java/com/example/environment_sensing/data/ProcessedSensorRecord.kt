//平均・中央値の処理済みデータEntity
package com.example.environment_sensing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ProcessedSensorRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val avgTemperature: Double,
    val medianTemperature: Double,
    val avgHumidity: Double,
    val medianHumidity: Double,
    val avgLight: Int,
    val medianLight: Int,
    val avgPressure: Double,
    val medianPressure: Double,
    val avgNoise: Double,
    val medianNoise: Double,
    val avgTvoc: Int,
    val medianTvoc: Int,
    val avgCo2: Int,
    val medianCo2: Int
)