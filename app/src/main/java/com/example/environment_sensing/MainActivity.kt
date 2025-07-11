package com.example.environment_sensing

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.bluetooth.le.ScanResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import pub.devrel.easypermissions.EasyPermissions
import kotlinx.coroutines.*
import com.example.environment_sensing.ui.theme.Environment_sensingTheme

class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var bleApi: BLEApi
    private lateinit var sensorLogger: SensorLogger

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastSavedTime = 0L

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bleApi = BLEApi()
        sensorLogger = SensorLogger(applicationContext, coroutineScope)

        setContent {
            Environment_sensingTheme {
                val navController = rememberNavController()
                var sensorData by remember { mutableStateOf<SensorData?>(null) }
                var rareMessage by remember { mutableStateOf("") }
                var normalMessage by remember { mutableStateOf("") }
                var showRareDialog by remember { mutableStateOf(false) }
                var showNormalDialog by remember { mutableStateOf(false) }

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
                            RealtimeScreen(
                                sensorData = sensorData,
                                rareMessage = rareMessage,
                                normalMessage = normalMessage,
                                showRareDialog = showRareDialog,
                                showNormalDialog = showNormalDialog,
                                onDismissRare = { showRareDialog = false },
                                onDismissNormal = { showNormalDialog = false },
                                onStartScan = {
                                    startScan { data ->
                                        sensorData = data

                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastSavedTime >= 10_000) {
                                            lastSavedTime = currentTime

                                            // 判定
                                            val rareName = RareEnvironmentChecker.check(data)
                                            if (rareName != null) {
                                                rareMessage = "🎉 レア環境ゲット！ [$rareName]"
                                                showRareDialog = true
                                            } else {
                                                val normalName = NormalEnvironmentChecker.check(data)
                                                if (normalName != null) {
                                                    normalMessage = "✨ [$normalName]"
                                                    showNormalDialog = true
                                                }
                                            }

                                            sensorLogger.log(data)
                                        }
                                    }
                                }
                            )
                        }
                        composable("history") {
                            HistoryScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d("permission", "許可された: $perms")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Log.d("permission", "拒否された: $perms")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun startScan(onDataParsed: (SensorData) -> Unit) {
        bleApi.startBLEBeaconScan(this) { beacon: ScanResult? ->
            val mac = beacon?.device?.address
            val advData = beacon?.scanRecord?.bytes
            if (mac == "C1:8B:A1:8E:26:FB") {
                advData?.let {
                    val data = parseAdvertisementData(it)
                    if (data != null) {
                        onDataParsed(data)
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