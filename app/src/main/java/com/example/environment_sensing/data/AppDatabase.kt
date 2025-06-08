//Roomデータベースの本体。全DAOをまとめる。
package com.example.environment_sensing.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SensorRawRecord::class, ProcessedSensorRecord::class],
    version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorRawDao(): SensorRawDao
    abstract fun processedSensorDao(): ProcessedSensorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sensor_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}