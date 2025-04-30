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
import androidx.compose.ui.unit.sp
import pub.devrel.easypermissions.EasyPermissions
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.SensorRecord
import kotlinx.coroutines.*

class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var bleApi: BLEApi
    private lateinit var database: AppDatabase
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastSavedTime = 0L

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bleApi = BLEApi()

        database = AppDatabase.getInstance(applicationContext)

        setContent {
            Environment_sensingTheme {
                var sensorData by remember { mutableStateOf<SensorData?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        Button(onClick = {
                            if (EasyPermissions.hasPermissions(this@MainActivity, *bleApi.permissions)) {
                                startScan { data ->
                                    sensorData = data

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastSavedTime >= 30_000) {
                                        lastSavedTime = currentTime
                                        coroutineScope.launch {
                                            val record = SensorRecord(
                                                timestamp = currentTime,
                                                temperature = data.temperature,
                                                humidity = data.humidity,
                                                light = data.light,
                                                pressure = data.pressure,
                                                noise = data.noise,
                                                tvoc = data.tvoc,
                                                co2 = data.co2
                                            )
                                            database.sensorDao().insert(record)
                                            Log.d("DB", "センサーデータ保存: $record")
                                        }
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
                            Text("🌡 気温: ${data.temperature}℃", fontSize = 30.sp)
                            Text("💧 湿度: ${data.humidity}%", fontSize = 30.sp)
                            Text("💡 照度: ${data.light} lx", fontSize = 30.sp)
                            Text("📈 気圧: ${data.pressure} hPa", fontSize = 30.sp)
                            Text("🔊 騒音: ${data.noise} dB", fontSize = 30.sp)
                            Text("🌫 TVOC: ${data.tvoc} ppb", fontSize = 30.sp)
                            Text("🌬 CO2: ${data.co2} ppm", fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d("permission", "許可された: $perms")
        startScan { /* パーミッションだけ通して、BLEは再度ボタン押してねでもOK */ }
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
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun startScan(onDataParsed: (SensorData) -> Unit) {
        bleApi.startBLEBeaconScan(this) { beacon: ScanResult? ->
            val mac = beacon?.device?.address
            val advData = beacon?.scanRecord?.bytes
            if (mac == "C1:8B:A1:8E:26:FB") {
                advData?.let {
                    Log.d("アドバタイズメントデータ", "${it.toHexString()}:data")
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
            val pressure = advData.getLittleEndianUInt16(16) / 10.0
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
}