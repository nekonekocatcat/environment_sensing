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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import com.example.environment_sensing.ui.theme.Environment_sensingTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

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

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Environment_sensingTheme {
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "realtime",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("realtime") {
                            var sensorData by remember { mutableStateOf<SensorData?>(null) }
                            var rareMessage by remember { mutableStateOf("") }
                            var normalMessage by remember { mutableStateOf("") }
                            var showRareDialog by remember { mutableStateOf(false) }
                            var showNormalDialog by remember { mutableStateOf(false) }

                            RealtimeScreen(
                                sensorData = sensorData,
                                rareMessage = rareMessage,
                                normalMessage = normalMessage,
                                onStartScan = {
                                    if (hasRequiredPermissions()) {
                                        if (hasRequiredPermissions()) {
                                            startLogService()
                                        } else {
                                            permissionLauncher.launch(REQUIRED_PERMISSIONS)
                                        }

                                        val bleApi = BLEApi()
                                        bleApi.startBLEBeaconScan(this@MainActivity) { beacon ->
                                            val advData = beacon?.scanRecord?.bytes
                                            if (beacon?.device?.address == "C1:8B:A1:8E:26:FB" && advData != null) {
                                                val data = parseAdvertisementData(advData)
                                                if (data != null) {
                                                    lifecycleScope.launch {
                                                        SensorEventBus.sensorData.emit(data)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        permissionLauncher.launch(REQUIRED_PERMISSIONS)
                                    }
                                },
                                showRareDialog = showRareDialog,
                                showNormalDialog = showNormalDialog,
                                onDismissRare = { showRareDialog = false },
                                onDismissNormal = { showNormalDialog = false }
                            )
                        }
                        composable("history") {
                            HistoryScreen()
                        }
                        composable("collection") {
                            CollectionScreen()
                        }
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