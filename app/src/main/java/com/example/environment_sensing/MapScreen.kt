package com.example.environment_sensing

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.maps.android.compose.*
import androidx.compose.ui.platform.LocalContext
import com.example.environment_sensing.data.AppDatabase
import com.google.android.gms.maps.model.*


private const val PIN_SIZE_DP = 50f

@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val pins by vm.pins.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        backfillMissingLocationsOnce(context)
        val db = AppDatabase.getInstance(context)
        android.util.Log.d(
            "MapBackfill",
            "missing normal=${db.normalEnvironmentLogDao().countMissingNormal()}, " +
                    "rare=${db.rareEnvironmentLogDao().countMissingRare()}"
        )
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
            locationProvider.currentOrNull()?.let {
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.lat, it.lon), 16f
                    )
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
        var mapReady by remember { mutableStateOf(false) }
        var rareIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
        var normalIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

        MapEffect {
            if (!mapReady) {
                mapReady = true
                rareIcon = bitmapDescriptorFromVector(
                    context, R.drawable.ic_pin, 0xFFE91E63.toInt(), sizeDp = PIN_SIZE_DP
                )
                normalIcon = bitmapDescriptorFromVector(
                    context, R.drawable.ic_pin, 0xFF1E88E5.toInt(), sizeDp = PIN_SIZE_DP
                )
            }
        }


        if (mapReady && rareIcon != null && normalIcon != null) {
            pins.forEach { pin ->
                Marker(
                    state = MarkerState(LatLng(pin.lat, pin.lon)),
                    title = pin.title,
                    snippet = pin.snippet,
                    icon = if (pin.isRare) rareIcon else normalIcon,
                    anchor = Offset(0.5f, 1f)
                )
            }
        }
    }
}