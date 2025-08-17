package com.example.environment_sensing
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import java.util.UUID
import pub.devrel.easypermissions.EasyPermissions

class BLEApi {
    //パーミッション確認用のコード
    private val PERMISSION_REQUEST_CODE = 1
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    var leScanCallback: ScanCallback? = null
    val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )
    }else{
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
    fun getPermission(context: Context, activity: Activity){
        //パーミッション確認
        if (!EasyPermissions.hasPermissions(context, *permissions)) {
            // パーミッションが許可されていない時の処理
            EasyPermissions.requestPermissions(activity, "パーミッションに関する説明", PERMISSION_REQUEST_CODE, *permissions)
        }
    }
    @SuppressLint("MissingPermission")
    fun startBLEBeaconScan(context: Context, resultBeacon: (ScanResult?) -> Unit) {
        leScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                resultBeacon(result)
            }
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        if (EasyPermissions.hasPermissions(context, *permissions)) {
            bluetoothLeScanner?.startScan(null, scanSettings, leScanCallback)
        } else {
            android.util.Log.e("BLE", "Permissions not granted, cannot start scan")
        }
    }
    @SuppressLint("MissingPermission")
    fun stopBLEBeaconScan(){
        bluetoothLeScanner?.stopScan(leScanCallback)
    }
}