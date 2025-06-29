package com.example.environment_sensing

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.environment_sensing.ui.theme.Environment_sensingTheme
import android.bluetooth.le.ScanResult
import pub.devrel.easypermissions.EasyPermissions
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var bleApi: BLEApi
    private lateinit var sensorLogger: SensorLogger

    private var rareMessage by mutableStateOf("")
    private val rareMessageDuration = 5_000L

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastSavedTime = 0L

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bleApi = BLEApi()

        // コールバックでレア環境通知を受け取るSensorLogger
        sensorLogger = SensorLogger(applicationContext, coroutineScope) { rareName ->
            rareMessage = "🎉 レア環境ゲット！ [$rareName]"
            coroutineScope.launch {
                delay(rareMessageDuration)
                rareMessage = ""
            }
        }

        setContent {
            Environment_sensingTheme {
                var sensorData by remember { mutableStateOf<SensorData?>(null) }
                val scrollState = rememberScrollState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Button(onClick = {
                            if (EasyPermissions.hasPermissions(this@MainActivity, *bleApi.permissions)) {
                                startScan { data ->
                                    sensorData = data

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastSavedTime >= 10_000) {
                                        lastSavedTime = currentTime
                                        sensorLogger.log(data)
                                    }
                                }
                            } else {
                                EasyPermissions.requestPermissions(
                                    this@MainActivity,
                                    "BLEスキャンにはパーミッションが必要です",
                                    1,
                                    *bleApi.permissions
                                )
                            }
                        }) {
                            Text("BLEスキャン開始")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        sensorData?.let { data ->
                            Text("🌡 気温: ${"%.1f".format(data.temperature)}℃", fontSize = 24.sp)
                            Text("💧 湿度: ${"%.1f".format(data.humidity)}%", fontSize = 24.sp)
                            Text("💡 照度: ${data.light} lx", fontSize = 24.sp)
                            Text("📈 気圧: ${"%.1f".format(data.pressure)} hPa", fontSize = 24.sp)
                            Text("🔊 騒音: ${"%.1f".format(data.noise)} dB", fontSize = 24.sp)
                            Text("🌫 TVOC: ${data.tvoc} ppb", fontSize = 24.sp)
                            Text("🌬 CO2: ${data.co2} ppm", fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (rareMessage.isNotEmpty()) {
                            Text(rareMessage, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this@MainActivity
        )
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