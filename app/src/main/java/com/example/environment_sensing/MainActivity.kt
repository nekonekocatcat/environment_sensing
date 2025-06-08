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
import com.example.environment_sensing.data.AppDatabase
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import com.example.environment_sensing.processing.SensorDataProcessor

class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var bleApi: BLEApi
    private lateinit var database: AppDatabase
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastSavedTime = 0L
    private lateinit var sensorLogger: SensorLogger
    private lateinit var processor: SensorDataProcessor

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bleApi = BLEApi()

        database = AppDatabase.getInstance(applicationContext)
        sensorLogger = SensorLogger(applicationContext, coroutineScope)
        processor = SensorDataProcessor()

        setContent {
            Environment_sensingTheme {
                var sensorData by remember { mutableStateOf<SensorData?>(null) }
                val scrollState = rememberScrollState()

                val sensorRawDataList by database.sensorRawDao().getAllFlow().collectAsState(initial = emptyList())
                val processedDataList by database.processedSensorDao().getAllFlow().collectAsState(initial = emptyList())

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
                                    }
                                    sensorLogger.log(data)

                                    coroutineScope.launch {
                                        val rawRecords = database.sensorRawDao().getRecentRecords(3)
                                        val processed = processor.process(rawRecords)
                                        processed?.let {
                                            database.processedSensorDao().insert(it)
                                            Log.d("DB", "処理済みデータ保存: $it")
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

                        // 最新センサーデータの表示
                        sensorData?.let { data ->
                            Text("🌡 気温: ${data.temperature}℃", fontSize = 30.sp)
                            Text("💧 湿度: ${data.humidity}%", fontSize = 30.sp)
                            Text("💡 照度: ${data.light} lx", fontSize = 30.sp)
                            Text("📈 気圧: ${data.pressure} hPa", fontSize = 30.sp)
                            Text("🔊 騒音: ${data.noise} dB", fontSize = 30.sp)
                            Text("🌫 TVOC: ${data.tvoc} ppb", fontSize = 30.sp)
                            Text("🌬 CO2: ${data.co2} ppm", fontSize = 30.sp)
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        Text("📈 10秒ごとのデータ一覧", fontSize = 24.sp)

                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(sensorRawDataList) { record ->
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text("🕒 時間: ${formatTimestamp(record.timestamp)}", fontSize = 16.sp)
                                    Text("🌡 気温: ${record.temperature}℃", fontSize = 16.sp)
                                    Text("💧 湿度: ${record.humidity}%", fontSize = 16.sp)
                                    Text("💡 照度: ${record.light} lx", fontSize = 16.sp)
                                    Text("📈 気圧: ${record.pressure} hPa", fontSize = 16.sp)
                                    Text("🔊 騒音: ${record.noise} dB", fontSize = 16.sp)
                                    Text("🌫 TVOC: ${record.tvoc} ppb", fontSize = 16.sp)
                                    Text("🌬 CO2: ${record.co2} ppm", fontSize = 16.sp)
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        Text("🧮 処理済みデータ一覧", fontSize = 24.sp)

                        LazyColumn(modifier = Modifier.height(400.dp)) {
                            items(processedDataList) { record ->
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("🕒 時間: ${formatTimestamp(record.timestamp)}", fontSize = 16.sp)
                                    Text("🌡 平均気温: ${record.avgTemperature} ℃", fontSize = 16.sp)
                                    Text("🌡 中央気温: ${record.medianTemperature} ℃", fontSize = 16.sp)
                                    Text("💧 平均湿度: ${record.avgHumidity} %", fontSize = 16.sp)
                                    Text("💧 中央湿度: ${record.medianHumidity} %", fontSize = 16.sp)
                                    Text("🔊 平均騒音: ${record.avgNoise} dB", fontSize = 16.sp)
                                    Text("🔊 中央騒音: ${record.medianNoise} dB", fontSize = 16.sp)
                                    Text("⛰ 平均気圧: ${record.avgPressure} hPa", fontSize = 16.sp)
                                    Text("⛰ 中央気圧: ${record.medianPressure} hPa", fontSize = 16.sp)
                                    Text("💡 平均照度: ${record.avgLight} lx", fontSize = 16.sp)
                                    Text("💡 中央照度: ${record.medianLight} lx", fontSize = 16.sp)
                                    Text("🌫 平均TVOC: ${record.avgTvoc} ppb", fontSize = 16.sp)
                                    Text("🌫 中央TVOC: ${record.medianTvoc} ppb", fontSize = 16.sp)
                                    Text("🫁 平均CO2: ${record.avgCo2} ppm", fontSize = 16.sp)
                                    Text("🫁 中央CO2: ${record.medianCo2} ppm", fontSize = 16.sp)
                                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
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