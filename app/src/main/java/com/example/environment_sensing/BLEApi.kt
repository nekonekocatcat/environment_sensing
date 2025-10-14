package com.example.environment_sensing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import pub.devrel.easypermissions.EasyPermissions

class BLEApi {
    private val PERMISSION_REQUEST_CODE = 1
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    var leScanCallback: ScanCallback? = null

    // ✅ OSごとに要求する権限を分ける
    private val permissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

    // 欠けている権限をリストアップ
    private fun missingPermissions(context: Context): List<String> =
        permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun getPermission(context: Context, activity: Activity) {
        val missing = missingPermissions(context)
        if (missing.isNotEmpty()) {
            Log.w("BLEApi", "missing perms: $missing")
            EasyPermissions.requestPermissions(
                activity,
                "BLEスキャンのために権限が必要です",
                PERMISSION_REQUEST_CODE,
                *missing.toTypedArray()
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startBLEBeaconScan(context: Context, resultBeacon: (ScanResult?) -> Unit) {
        // 端末でBT無効なら即終了
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w("BLEApi", "Bluetooth is disabled or adapter is null")
            return
        }

        val missing = missingPermissions(context)
        if (missing.isNotEmpty()) {
            Log.w("BLEApi", "cannot start scan, missing=$missing")
            return
        }

        leScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                resultBeacon(result)
            }
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        bluetoothLeScanner?.startScan(null, scanSettings, leScanCallback)
        Log.d("BLEApi", "startScan() requested")
    }

    @SuppressLint("MissingPermission")
    fun stopBLEBeaconScan() {
        bluetoothLeScanner?.stopScan(leScanCallback)
        leScanCallback = null
    }
}