package com.example.environment_sensing.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationClient(context: Context) {
    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val last = try { fused.lastLocation.awaitOrNull() } catch (_: Exception) { null }
        if (last != null) return last

        return null
    }
}

// Task<Location> をコルーチンで待つ簡易拡張
private suspend fun com.google.android.gms.tasks.Task<Location>.awaitOrNull(): Location? =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
        addOnCanceledListener { cont.resume(null) }
    }