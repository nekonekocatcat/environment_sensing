//Roomデータベースの本体。全DAOをまとめる。
package com.example.environment_sensing.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import androidx.room.migration.Migration

@Database(
    entities = [
        SensorRawRecord::class,
        ProcessedSensorRecord::class,
        RareEnvironmentLog::class,
        NormalEnvironmentLog::class,
        EnvironmentCollection::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorRawDao(): SensorRawDao
    abstract fun processedSensorDao(): ProcessedSensorDao
    abstract fun rareEnvironmentLogDao(): RareEnvironmentLogDao
    abstract fun environmentCollectionDao(): EnvironmentCollectionDao
    abstract fun normalEnvironmentLogDao(): NormalEnvironmentLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // ★ Migration を定義
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE SensorRawRecord ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE SensorRawRecord ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE RareEnvironmentLog ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE RareEnvironmentLog ADD COLUMN longitude REAL")
                db.execSQL("ALTER TABLE NormalEnvironmentLog ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE NormalEnvironmentLog ADD COLUMN longitude REAL")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sensor_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}