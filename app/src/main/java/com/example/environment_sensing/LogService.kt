package com.example.environment_sensing

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*

class LogService : Service() {
    companion object {
        const val CHANNEL_ID = "12345"
    }

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var bleApi: BLEApi
    private lateinit var sensorLogger: SensorLogger
    private var lastSavedTime = 0L

    override fun onCreate() {
        super.onCreate()
        bleApi = BLEApi()
        sensorLogger = SensorLogger(
            applicationContext,
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
            onRareDetected = { showNotification("レア環境ゲット！", it) },
            onNormalDetected = { showNotification("ノーマル環境ゲット！", it) }
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = NotificationManagerCompat.from(this)
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("環境ログ出力")
            .build()
        notificationManager.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("環境データ取得中")
            .setContentText("バックグラウンドで環境を監視しています")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
        startForeground(1212, notification)

        startBLEScan()
        return START_STICKY
    }

    private fun startBLEScan() {
        bleApi.startBLEBeaconScan(this) { beacon: ScanResult? ->
            val mac = beacon?.device?.address
            val advData = beacon?.scanRecord?.bytes
            if (mac == "C1:8B:A1:8E:26:FB") {
                advData?.let {
                    val data = parseAdvertisementData(it)
                    if (data != null) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastSavedTime >= 10_000) {
                            lastSavedTime = currentTime
                            sensorLogger.log(data)
                        }
                    }
                }
            }
        }
    }

    private fun parseAdvertisementData(advData: ByteArray): SensorData? {
        return try {
            val temperature = advData.getLittleEndianUInt16(9) / 100.0
            val humidity = advData.getLittleEndianUInt16(11) / 100.0
            val light = advData.getLittleEndianUInt16(13)
            val pressure = advData.getLittleEndianUInt24(15) / 1000.0
            val noise = advData.getLittleEndianUInt16(19) / 100.0
            val tvoc = advData.getLittleEndianUInt16(21)
            val co2 = advData.getLittleEndianUInt16(23)
            SensorData(temperature, humidity, light, pressure, noise, tvoc, co2)
        } catch (e: Exception) {
            Log.e("parseError", "パース失敗: ${e.message}")
            null
        }
    }

    private fun ByteArray.getLittleEndianUInt16(index: Int) =
        (this[index].toInt() and 0xFF) or ((this[index + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.getLittleEndianUInt24(index: Int) =
        (this[index].toInt() and 0xFF) or
                ((this[index + 1].toInt() and 0xFF) shl 8) or
                ((this[index + 2].toInt() and 0xFF) shl 16)

    private fun showNotification(title: String, message: String) {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify((0..9999).random(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}