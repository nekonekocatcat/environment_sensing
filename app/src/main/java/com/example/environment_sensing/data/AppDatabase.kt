//Roomデータベースの本体。全DAOをまとめる。
package com.example.environment_sensing.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [
        SensorRawRecord::class,
        ProcessedSensorRecord::class,
        RareEnvironmentLog::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorRawDao(): SensorRawDao
    abstract fun processedSensorDao(): ProcessedSensorDao
    abstract fun rareEnvironmentLogDao(): RareEnvironmentLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sensor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}