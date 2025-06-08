package com.example.environment_sensing

import com.example.environment_sensing.data.ProcessedSensorRecord
import com.example.environment_sensing.data.SensorRawRecord

class SensorDataProcessor {
    fun process(records: List<SensorRawRecord>): ProcessedSensorRecord? {
        if (records.size < 3) return null

        val timestamp = System.currentTimeMillis()

        fun <T : Comparable<T>> List<T>.median(): T {
            return sorted()[size / 2]
        }

        return ProcessedSensorRecord(
            timestamp = timestamp,
            avgTemperature = records.map { it.temperature }.average(),
            medianTemperature = records.map { it.temperature }.median(),
            avgHumidity = records.map { it.humidity }.average(),
            medianHumidity = records.map { it.humidity }.median(),
            avgLight = records.map { it.light }.average().toInt(),
            medianLight = records.map { it.light }.median(),
            avgPressure = records.map { it.pressure }.average(),
            medianPressure = records.map { it.pressure }.median(),
            avgNoise = records.map { it.noise }.average(),
            medianNoise = records.map { it.noise }.median(),
            avgTvoc = records.map { it.tvoc }.average().toInt(),
            medianTvoc = records.map { it.tvoc }.median(),
            avgCo2 = records.map { it.co2 }.average().toInt(),
            medianCo2 = records.map { it.co2 }.median()
        )
    }
}