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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.sp
import pub.devrel.easypermissions.EasyPermissions
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.environment_sensing.data.SensorRawRecord

class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var bleApi: BLEApi
    private lateinit var sensorLogger: SensorLogger
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastSavedTime = 0L
    private val csvFileName = "sensor_raw_data.csv"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bleApi = BLEApi()
        sensorLogger = SensorLogger(applicationContext, coroutineScope)

        setContent {
            Environment_sensingTheme {
                var sensorData by remember { mutableStateOf<SensorData?>(null) }
                var processedText by remember { mutableStateOf("") }
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
                                    if (currentTime - lastSavedTime >= 30_000) {
                                        lastSavedTime = currentTime
                                        coroutineScope.launch {
                                            val recentData = readLastSensorDataFromCsv(3)
                                            if (recentData.size == 3) {
                                                val sensorRawList = recentData.map {
                                                    SensorRawRecord(
                                                        timestamp = System.currentTimeMillis(), // CSVに時間がないのでここで代用
                                                        temperature = it.temperature,
                                                        humidity = it.humidity,
                                                        light = it.light,
                                                        pressure = it.pressure,
                                                        noise = it.noise,
                                                        tvoc = it.tvoc,
                                                        co2 = it.co2
                                                    )
                                                }
                                                val processed = SensorDataProcessor().process(sensorRawList)
                                                processed?.let {
                                                    val text = """
                                                    🕒 ${formatTimestamp(it.timestamp)}
                                                    🌡 平均気温: ${it.avgTemperature} / 中央気温: ${it.medianTemperature}
                                                    💧 平均湿度: ${it.avgHumidity} / 中央湿度: ${it.medianHumidity}
                                                    💡 平均照度: ${it.avgLight} / 中央照度: ${it.medianLight}
                                                    📈 平均気圧: ${it.avgPressure} / 中央気圧: ${it.medianPressure}
                                                    🔊 平均騒音: ${it.avgNoise} / 中央騒音: ${it.medianNoise}
                                                    🌫 平均TVOC: ${it.avgTvoc} / 中央TVOC: ${it.medianTvoc}
                                                    🫁 平均CO2: ${it.avgCo2} / 中央CO2: ${it.medianCo2}
                                                    """.trimIndent()
                                                    processedText = text
                                                }
                                            }
                                        }
                                    }

                                    sensorLogger.log(data)
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
                            Text("🌡 気温: ${data.temperature}℃", fontSize = 24.sp)
                            Text("💧 湿度: ${data.humidity}%", fontSize = 24.sp)
                            Text("💡 照度: ${data.light} lx", fontSize = 24.sp)
                            Text("📈 気圧: ${data.pressure} hPa", fontSize = 24.sp)
                            Text("🔊 騒音: ${data.noise} dB", fontSize = 24.sp)
                            Text("🌫 TVOC: ${data.tvoc} ppb", fontSize = 24.sp)
                            Text("🌬 CO2: ${data.co2} ppm", fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (processedText.isNotEmpty()) {
                            Text("🧮 処理済みデータ", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(processedText, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    private suspend fun readLastSensorDataFromCsv(n: Int): List<SensorData> {
        val file = File(applicationContext.getExternalFilesDir(null), csvFileName)
        if (!file.exists()) return emptyList()

        return withContext(Dispatchers.IO) {
            file.readLines()
                .drop(1) // ヘッダー除外
                .takeLast(n)
                .mapNotNull { line ->
                    val parts = line.split(",")
                    try {
                        SensorData(
                            temperature = parts[1].toDouble(),
                            humidity = parts[2].toDouble(),
                            light = parts[3].toInt(),
                            pressure = parts[4].toDouble(),
                            noise = parts[5].toDouble(),
                            tvoc = parts[6].toInt(),
                            co2 = parts[7].toInt()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
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