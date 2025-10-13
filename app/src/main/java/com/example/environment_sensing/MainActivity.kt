package com.example.environment_sensing

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.EnvironmentCollection
import com.example.environment_sensing.ui.theme.Environment_sensingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var lastShownRare = mutableMapOf<String, Long>()
    private var lastShownNormal = mutableMapOf<String, Long>()
    private val cooldownMillis = 10_000L

    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.FOREGROUND_SERVICE,
        android.Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                startLogService()
            } else {
                Toast.makeText(this, "必要な権限が許可されませんでした", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        // バッテリー最適化解除
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        //ここ絶対本番環境で削除して絶対に❣️
        applicationContext.deleteDatabase("sensor_database")

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Environment_sensingTheme {
                val realtimeVM: RealtimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                var simpleMode by rememberSaveable { mutableStateOf(false) }
                val navController = rememberNavController()


                LaunchedEffect(navController) {
                    launch {
                        SensorEventBus.rareFirstEvent.collect { name ->
                            android.util.Log.d("AutoNav", "RARE first-time: $name -> navigate")
                            navController.navigate("collection") {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo("realtime") { saveState = true }
                            }
                        }
                    }
                    launch {
                        SensorEventBus.normalFirstEvent.collect { name ->
                            android.util.Log.d("AutoNav", "NORMAL first-time: $name -> navigate")
                            navController.navigate("collection") {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo("realtime") { saveState = true }
                            }
                        }
                    }
                }


                Scaffold(
                    bottomBar = { if (!simpleMode) BottomNavigationBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "realtime",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("realtime") {
                            if (simpleMode) {
                                //  実験モード（数値のみ表示）
                                SimpleRealtimeScreen(
                                    viewModel = realtimeVM,
                                    isScanning = realtimeVM.isScanning.collectAsState().value,
                                    onToggleScan = { enable ->
                                        if (enable) {
                                            if (hasRequiredPermissions()) startLogService()
                                            else permissionLauncher.launch(REQUIRED_PERMISSIONS)
                                        } else {
                                            stopService(Intent(this@MainActivity, LogService::class.java))
                                        }
                                        realtimeVM.setScanning(enable)
                                    },
                                    onBackToFull = { simpleMode = false }
                                )
                            } else {
                                //  通常モード（今までのリアルタイム表示）
                                RealtimeScreen(
                                    viewModel = realtimeVM,
                                    onToggleScan = { enable ->
                                        if (enable) {
                                            if (hasRequiredPermissions()) startLogService()
                                            else permissionLauncher.launch(REQUIRED_PERMISSIONS)
                                        } else {
                                            stopService(Intent(this@MainActivity, LogService::class.java))
                                        }
                                        realtimeVM.setScanning(enable)
                                    },
                                    onSwitchToSimple = { simpleMode = true }
                                )
                            }
                        }
                        composable("history") { HistoryScreen() }
                        composable("collection") { CollectionScreen() }
                    }
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startLogService() {
        val intent = Intent(application, LogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
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
            Log.e("parseError", "パースに失敗: ${e.message}")
            null
        }
    }

    private fun ByteArray.getLittleEndianUInt16(index: Int): Int {
        return (this[index].toInt() and 0xFF) or ((this[index + 1].toInt() and 0xFF) shl 8)
    }

    private fun ByteArray.getLittleEndianUInt24(index: Int): Int {
        return (this[index].toInt() and 0xFF) or
                ((this[index + 1].toInt() and 0xFF) shl 8) or
                ((this[index + 2].toInt() and 0xFF) shl 16)
    }

}