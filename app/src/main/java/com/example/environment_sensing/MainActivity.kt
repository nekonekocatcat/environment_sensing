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

                            // パーミッションがあるかチェック
                            if (EasyPermissions.hasPermissions(this, *bleApi.permissions)) {
                                startScan()  // スキャン開始！
                            } else {
                                // パーミッションを要求
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

    // パーミッションが許可された時に呼ばれる
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d("permission", "許可された: $perms")
        startScan()  // 許可された後にスキャン開始！
    }

    // 拒否された場合
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Log.d("permission", "拒否された: $perms")
    }

    // EasyPermissionsがこれ必要！
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    // BLEスキャン処理（共通化）
    @OptIn(ExperimentalStdlibApi::class)
    private fun startScan() {
        bleApi.startBLEBeaconScan(this) { beacon: ScanResult? ->
            val mac = beacon?.device?.address
            val advData = beacon?.scanRecord?.bytes
            //Log.d("main", mac.toString())
            if (beacon?.device?.address == "C1:8B:A1:8E:26:FB") {
                if (advData != null) {
                    Log.d("アドバタイズメントデータ", "${advData.toHexString()}:data")
                }
            }
        }
    }
}