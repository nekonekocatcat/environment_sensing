package com.example.environment_sensing

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.EnvironmentCollection
import kotlinx.coroutines.*

class LogService : Service() {

    companion object {
        const val CHANNEL_ID = "env_log_channel"
        @Volatile var isRunning: Boolean = false
        @Volatile var lastBeaconElapsed: Long = 0L
    }

    private val allowedMacs = setOf(
        "C1:8B:A1:8E:26:FB"
    )

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var sensorLogger: SensorLogger
    private lateinit var locationProvider: LocationProvider
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var scanJob: Job? = null
    private var watchdogJob: Job? = null
    private var lastSavedTime = 0L

    private val bleScanner: BluetoothLeScanner? by lazy {
        BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner
    }

    private val scanFilters: List<ScanFilter> by lazy {
        if (allowedMacs.isEmpty()) emptyList()
        else allowedMacs.map { mac -> ScanFilter.Builder().setDeviceAddress(mac).build() }
    }

    private val scanSettings: ScanSettings by lazy {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(1000)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .build()
    }

    private var leCallback: ScanCallback? = null

    override fun onCreate() {
        super.onCreate()
        locationProvider = LocationProvider(applicationContext)
        try { locationProvider.startUpdates() } catch (_: Throwable) {}
        isRunning = true

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "envsensing:forever").apply {
            setReferenceCounted(false)
            acquire()
        }

        lastBeaconElapsed = SystemClock.elapsedRealtime()

        val appCtx = applicationContext
        val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        sensorLogger = SensorLogger(
            appCtx,
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
            onRareDetected = { name ->
                showNotification("レア環境ゲット！", name)
                ioScope.launch {
                    val dao = AppDatabase.getInstance(appCtx).environmentCollectionDao()
                    val isFirst = dao.countByName(name) == 0
                    if (isFirst) {
                        dao.insertIfNotExists(
                            EnvironmentCollection(
                                environmentName = name,
                                name = name,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        SensorEventBus.rareEvent.emit(name)
                        if (isFirst) SensorEventBus.rareFirstEvent.emit(name)
                    }
                }
            },
            onNormalDetected = { name ->
                showNotification("ノーマル環境ゲット！", name)
                ioScope.launch {
                    val dao = AppDatabase.getInstance(appCtx).environmentCollectionDao()
                    val isFirst = dao.countByName(name) == 0
                    if (isFirst) {
                        dao.insertIfNotExists(
                            EnvironmentCollection(
                                environmentName = name,
                                name = name,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        SensorEventBus.normalEvent.emit(name)
                        if (isFirst) SensorEventBus.normalFirstEvent.emit(name)
                    }
                }
            },
                    locationProvider = locationProvider
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = NotificationManagerCompat.from(this)
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW
        ).setName("環境ログ出力").build()
        notificationManager.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("環境データ取得中")
            .setContentText("停止するまでスキャンし続けます")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
        startForeground(1212, notification)

        startCallbackScan()

        scanJob?.cancel()
        scanJob = uiScope.launch {
            while (isRunning) {
                delay(2 * 60 * 1000L)
                try { restartCallbackScan() } catch (_: Throwable) {}
            }
        }


        watchdogJob?.cancel()
        watchdogJob = uiScope.launch {
            while (isRunning) {
                delay(10_000L)
                val idle = SystemClock.elapsedRealtime() - lastBeaconElapsed
                if (idle > 12_000L) {
                    try { restartCallbackScan() } catch (_: Throwable) {}
                }
            }
        }

        return START_STICKY
    }

    private fun startCallbackScan() {
        val scanner = bleScanner ?: return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("LogService", "BLUETOOTH_SCAN 権限なし")
            return
        }
        if (leCallback != null) return

        leCallback = object : ScanCallback() {
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                lastBeaconElapsed = SystemClock.elapsedRealtime()
                handleResults(results)
            }
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                lastBeaconElapsed = SystemClock.elapsedRealtime()
                handleResults(listOf(result))
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e("LogService", "scan failed: $errorCode")
            }
        }

        try {
            scanner.startScan(scanFilters, scanSettings, leCallback)
            Log.d("LogService", "startScan (callback)")
        } catch (e: Exception) {
            Log.e("LogService", "startScan failed: ${e.message}", e)
        }
    }

    private fun stopCallbackScan() {
        val scanner = bleScanner ?: return
        val cb = leCallback ?: return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) return
        try { scanner.stopScan(cb) } catch (_: Exception) {}
        leCallback = null
        Log.d("LogService", "stopScan (callback)")
    }

    private fun flushPending() {
        try {
            val scanner = bleScanner ?: return
            val cb = leCallback ?: return
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                == PackageManager.PERMISSION_GRANTED) {
                scanner.flushPendingScanResults(cb)
            }
        } catch (_: Exception) {}
    }

    private fun restartCallbackScan() {
        flushPending()
        stopCallbackScan()
        Thread.sleep(250)
        startCallbackScan()
    }

    private fun handleResults(results: List<ScanResult>) {
        results.forEach { beacon ->
            val mac = beacon.device?.address ?: return@forEach
            if (!allowedMacs.contains(mac)) return@forEach
            val adv = beacon.scanRecord?.bytes ?: return@forEach
            val data = parseAdvertisementData(adv) ?: return@forEach

            uiScope.launch { SensorEventBus.sensorData.emit(data) }

            // 10秒に1回DB/CSV保存
            val now = System.currentTimeMillis()
            if (now - lastSavedTime >= 10_000) {
                lastSavedTime = now
                sensorLogger.log(data)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { stopCallbackScan() } catch (_: Throwable) {}
        scanJob?.cancel()
        watchdogJob?.cancel()
        uiScope.cancel()
        try { wakeLock?.release() } catch (_: Throwable) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun parseAdvertisementData(advData: ByteArray): SensorData? = try {
        val temperature = advData.getLittleEndianUInt16(9) / 100.0
        val humidity    = advData.getLittleEndianUInt16(11) / 100.0
        val light       = advData.getLittleEndianUInt16(13)
        val pressure    = advData.getLittleEndianUInt24(15) / 1000.0
        val noise       = advData.getLittleEndianUInt16(19) / 100.0
        val tvoc        = advData.getLittleEndianUInt16(21)
        val co2         = advData.getLittleEndianUInt16(23)
        SensorData(temperature, humidity, light, pressure, noise, tvoc, co2)
    } catch (e: Exception) {
        Log.e("parseError", "パース失敗: ${e.message}")
        null
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        notificationManager.notify((0..9999).random(), notification)
    }
}