package com.example.environment_sensing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.environment_sensing.data.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MapPin(
    val lat: Double,
    val lon: Double,
    val title: String,
    val snippet: String,
    val isRare: Boolean
)

class MapViewModel(app: Application): AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    val pins: StateFlow<List<MapPin>> =
        combine(
            db.rareEnvironmentLogDao().observeAllWithLocation(),
            db.normalEnvironmentLogDao().observeAllWithLocation()
        ) { rare, normal ->
            val rarePins = rare.mapNotNull {
                val lat = it.latitude ?: return@mapNotNull null
                val lon = it.longitude ?: return@mapNotNull null
                MapPin(
                    lat, lon,
                    title = it.environmentName,
                    snippet = formatTime(it.timestamp),
                    isRare = true
                )
            }
            val normalPins = normal.mapNotNull {
                val lat = it.latitude ?: return@mapNotNull null
                val lon = it.longitude ?: return@mapNotNull null
                MapPin(
                    lat, lon,
                    title = it.environmentName,
                    snippet = formatTime(it.timestamp),
                    isRare = false
                )
            }
            rarePins + normalPins
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun formatTime(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }
}