package com.example.environment_sensing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicReference

data class GeoPoint(val lat: Double, val lon: Double)

class LocationProvider(private val appContext: Context) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    private val lastGood = AtomicReference<GeoPoint?>(null)

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            lastGood.set(GeoPoint(loc.latitude, loc.longitude))
        }
    }

    fun startUpdates() {
        if (!hasLocationPerm()) return
        val req = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            30_000L // 30秒間隔
        )
            .setMinUpdateIntervalMillis(10_000L) // 最短10秒
            .setWaitForAccurateLocation(false)
            .build()

        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
    }

    fun stopUpdates() {
        fused.removeLocationUpdates(callback)
    }

    suspend fun currentOrNull(): GeoPoint? {
        if (!hasLocationPerm()) return lastGood.get()
        lastGood.get()?.let { return it }
        return try {
            val loc = fused.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, null
            ).await() ?: fused.lastLocation.await()
            loc?.let { GeoPoint(it.latitude, it.longitude) }?.also { lastGood.set(it) }
        } catch (_: Exception) {
            lastGood.get()
        }
    }

    private fun hasLocationPerm(): Boolean {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}