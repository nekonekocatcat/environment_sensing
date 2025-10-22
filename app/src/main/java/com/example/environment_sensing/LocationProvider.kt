package com.example.environment_sensing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

data class GeoPoint(val lat: Double, val lon: Double)

class LocationProvider(private val appContext: Context) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    suspend fun currentOrNull(): GeoPoint? {
        val fine = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        return try {
            val loc = fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: fused.lastLocation.await()
            loc?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }
}