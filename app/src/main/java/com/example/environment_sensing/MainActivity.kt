package com.example.environment_sensing

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.environment_sensing.ui.theme.Environment_sensingTheme
import androidx.compose.foundation.layout.PaddingValues
import android.bluetooth.le.ScanResult
import androidx.compose.ui.unit.dp
import pub.devrel.easypermissions.EasyPermissions

fun ByteArray.getLittleEndianUInt16(index: Int): Int {
    return (this[index].toInt() and 0xFF) or ((this[index + 1].toInt() and 0xFF) shl 8)
}

class MainActivity : ComponentActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var bleApi: BLEApi

    @OptIn(ExperimentalStdlibApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bleApi = BLEApi()

        setContent {
            Environment_sensingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Button(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp),
                        onClick = {
                            Log.d("main", "スキャンボタン押された")

                            if (EasyPermissions.hasPermissions(this, *bleApi.permissions)) {
                                startScan()
                            } else {
                                EasyPermissions.requestPermissions(
                                    this,
                                    "BLEスキャンにはパーミッションが必要です",
                                    1,
                                    *bleApi.permissions
                                )
                            }
                        }) {
                        Text("BLEスキャン開始")
                    }
                }
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d("permission", "許可された: $perms")
        startScan()
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


    private fun parseAdvertisementData(advData: ByteArray) {
        try {
            val temperatureRaw = advData.getLittleEndianUInt16(9)
            val humidityRaw = advData.getLittleEndianUInt16(11)
            val lightRaw = advData.getLittleEndianUInt16(13)
            val pressureRaw = advData.getLittleEndianUInt16(16)
            val noiseRaw = advData.getLittleEndianUInt16(19)
            val tvocRaw = advData.getLittleEndianUInt16(21)
            val co2Raw = advData.getLittleEndianUInt16(23)

            val temperature = temperatureRaw / 100.0
            val humidity = humidityRaw / 100.0
            val light = lightRaw
            val pressure = pressureRaw / 10.0
            val noise = noiseRaw / 100.0
            val tvoc = tvocRaw
            val co2 = co2Raw

            Log.d("センサーデータ", "🌡 気温: ${temperature}℃")
            Log.d("センサーデータ", "💧 湿度: ${humidity}%")
            Log.d("センサーデータ", "💡 照度: ${light} lx")
            Log.d("センサーデータ", "📈 気圧: ${pressure} hPa")
            Log.d("センサーデータ", "🔊 騒音: ${noise} dB")
            Log.d("センサーデータ", "🌫 TVOC: ${tvoc} ppb")
            Log.d("センサーデータ", "🌬 CO2: ${co2} ppm")

        } catch (e: Exception) {
            Log.e("parseError", "パースに失敗: ${e.message}")
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    private fun startScan() {
        bleApi.startBLEBeaconScan(this) { beacon: ScanResult? ->
            val mac = beacon?.device?.address
            val advData = beacon?.scanRecord?.bytes
            if (mac == "C1:8B:A1:8E:26:FB") {
                if (advData != null) {
                    Log.d("アドバタイズメントデータ", "${advData.toHexString()}:data")
                    parseAdvertisementData(advData)
                }
            }
        }
    }
}