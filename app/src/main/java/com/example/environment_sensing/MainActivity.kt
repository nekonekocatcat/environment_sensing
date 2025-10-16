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

    private fun runtimePermissions(): Array<String> {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += android.Manifest.permission.BLUETOOTH_SCAN
            perms += android.Manifest.permission.BLUETOOTH_CONNECT
            perms += android.Manifest.permission.ACCESS_FINE_LOCATION
            perms += android.Manifest.permission.ACCESS_COARSE_LOCATION
        } else {
            perms += android.Manifest.permission.ACCESS_FINE_LOCATION
            perms += android.Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += android.Manifest.permission.POST_NOTIFICATIONS
        }
        return perms.toTypedArray()
    }

    private fun missingPermissions(): List<String> =
        runtimePermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }


    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val bleMissing = missing(blePermissions())
            if (bleMissing.isEmpty()) {
                startLogService()
            } else {
                Toast.makeText(this, "必要な権限が許可されませんでした", Toast.LENGTH_SHORT).show()
                Log.w("PERM", "still missing BLE perms: $bleMissing")
            }
        }

    private fun blePermissions(): Array<String> {
        val p = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            p += android.Manifest.permission.BLUETOOTH_SCAN
            p += android.Manifest.permission.BLUETOOTH_CONNECT
            p += android.Manifest.permission.ACCESS_FINE_LOCATION
            p += android.Manifest.permission.ACCESS_COARSE_LOCATION
        } else {
            p += android.Manifest.permission.ACCESS_FINE_LOCATION
            p += android.Manifest.permission.ACCESS_COARSE_LOCATION
        }
        return p.toTypedArray()
    }

    private fun isGranted(perm: String) =
        ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

    private fun missing(perms: Array<String>) =
        perms.filterNot { isGranted(it) }

    private fun needsNotifPermissionRequest(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !isGranted(android.Manifest.permission.POST_NOTIFICATIONS)



    override fun onCreate(savedInstanceState: Bundle?) {
        // バッテリー最適化解除
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        Log.d("PERM", "sdk=${Build.VERSION.SDK_INT}, missing=${missingPermissions()}")


        //ここ絶対本番環境で削除して絶対に❣️
        //applicationContext.deleteDatabase("sensor_database")

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Environment_sensingTheme {
                val realtimeVM: RealtimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                var simpleMode by rememberSaveable { mutableStateOf(false) }
                val navController = rememberNavController()


                LaunchedEffect(navController, simpleMode) {
                    // レア環境
                    launch {
                        SensorEventBus.rareFirstEvent.collect { name ->
                            if (!simpleMode) {
                                android.util.Log.d("AutoNav", "RARE first-time: $name -> navigate")
                                navController.navigate("collection") {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo("realtime") { saveState = true }
                                }
                            }
                        }
                    }
                    // ノーマル環境
                    launch {
                        SensorEventBus.normalFirstEvent.collect { name ->
                            if (!simpleMode) {
                                android.util.Log.d("AutoNav", "NORMAL first-time: $name -> navigate")
                                navController.navigate("collection") {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo("realtime") { saveState = true }
                                }
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
                                            val bleMissing = missing(blePermissions())
                                            val requestList = mutableListOf<String>().apply {
                                                addAll(bleMissing)
                                                if (needsNotifPermissionRequest()) {
                                                    add(android.Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                            if (requestList.isNotEmpty()) {
                                                permissionLauncher.launch(requestList.toTypedArray())
                                            } else {
                                                startLogService()
                                            }
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
                                            val bleMissing = missing(blePermissions())
                                            val requestList = mutableListOf<String>().apply {
                                                addAll(bleMissing)
                                                if (needsNotifPermissionRequest()) {
                                                    add(android.Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                            if (requestList.isNotEmpty()) {
                                                permissionLauncher.launch(requestList.toTypedArray())
                                            } else {
                                                startLogService()
                                            }
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