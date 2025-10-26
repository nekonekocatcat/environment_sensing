package com.example.environment_sensing

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.maps.android.compose.*
import androidx.compose.ui.platform.LocalContext
import com.example.environment_sensing.data.AppDatabase
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val pins by vm.pins.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // まず足りない座標を後追い埋め
        backfillMissingLocationsOnce(context)

        // （任意）デバッグ：残り件数をログる
        val db = AppDatabase.getInstance(context)
        val missN = db.normalEnvironmentLogDao().countMissingNormal()
        val missR = db.rareEnvironmentLogDao().countMissingRare()
        android.util.Log.d("MapBackfill", "missing normal=$missN, rare=$missR")
    }

    // 位置権限
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val myLocationEnabled = hasFine || hasCoarse

    // 初期フォールバック：最後のピン or 東京駅
    val fallback = pins.lastOrNull()?.let { LatLng(it.lat, it.lon) } ?: LatLng(35.681236, 139.767125)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fallback, 13f)
    }

    val locationProvider = remember { LocationProvider(context.applicationContext) }
    var movedToMyLocation by remember { mutableStateOf(false) }

    LaunchedEffect(myLocationEnabled) {
        if (myLocationEnabled && !movedToMyLocation) {
            val gp = locationProvider.currentOrNull()
            val here = gp?.let { LatLng(it.lat, it.lon) }
            if (here != null) {
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(here, 16f)
                )
                movedToMyLocation = true
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(zoomControlsEnabled = true),
        properties = MapProperties(isMyLocationEnabled = myLocationEnabled)
    ) {
        pins.forEach { pin ->
            val color = if (pin.isRare) BitmapDescriptorFactory.HUE_ROSE else BitmapDescriptorFactory.HUE_AZURE
            Marker(
                state = MarkerState(LatLng(pin.lat, pin.lon)),
                title = pin.title,
                snippet = pin.snippet,
                icon = BitmapDescriptorFactory.defaultMarker(color)
            )
        }
    }
}