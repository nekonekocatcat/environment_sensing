package com.example.environment_sensing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.environment_sensing.data.AppDatabase
import kotlinx.coroutines.flow.*

data class MapFilter(
    val kind: Kind = Kind.All,
    val tiers: Set<Int> = emptySet(),
    val timeRange: TimeRange = TimeRange.All,
    val keyword: String = ""
) {
    enum class Kind { All, RareOnly, NormalOnly }
    enum class TimeRange { Last24h, Last7d, Last30d, All }
}

data class MapPin(
    val lat: Double,
    val lon: Double,
    val title: String,
    val snippet: String,
    val isRare: Boolean,
    val tier: Int?,
    val timestamp: Long
)

class MapViewModel(app: Application): AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    private val tierMap: Map<String, Int> =
        RareEnvironmentChecker.environments.associate { it.name to it.tier }

    private val _filter = MutableStateFlow(MapFilter())
    val filter: StateFlow<MapFilter> = _filter

    fun updateFilter(update: (MapFilter) -> MapFilter) {
        _filter.value = update(_filter.value)
    }

    val pins: StateFlow<List<MapPin>> =
        combine(
            db.rareEnvironmentLogDao().observeAllWithLocation(),
            db.normalEnvironmentLogDao().observeAllWithLocation(),
            filter
        ) { rare, normal, f ->

            fun toPin(name: String, lat: Double, lon: Double, ts: Long, rare: Boolean): MapPin =
                MapPin(
                    lat = lat,
                    lon = lon,
                    title = name,
                    snippet = formatTime(ts),
                    isRare = rare,
                    tier = if (rare) tierMap[name] else null,
                    timestamp = ts
                )

            val rarePins = rare.mapNotNull {
                val lat = it.latitude ?: return@mapNotNull null
                val lon = it.longitude ?: return@mapNotNull null
                toPin(it.environmentName, lat, lon, it.timestamp, true)
            }

            val normalPins = normal.mapNotNull {
                val lat = it.latitude ?: return@mapNotNull null
                val lon = it.longitude ?: return@mapNotNull null
                toPin(it.environmentName, lat, lon, it.timestamp, false)
            }

            (rarePins + normalPins)
                .filter { pin ->
                    when (f.kind) {
                        MapFilter.Kind.All -> true
                        MapFilter.Kind.RareOnly -> pin.isRare
                        MapFilter.Kind.NormalOnly -> !pin.isRare
                    }
                }
                .filter { pin ->
                    if (!pin.isRare) true
                    else if (f.tiers.isEmpty()) true
                    else pin.tier?.let { it in f.tiers } ?: false
                }
                .filter { pin ->
                    val now = System.currentTimeMillis()
                    val from = when (f.timeRange) {
                        MapFilter.TimeRange.All -> 0L
                        MapFilter.TimeRange.Last24h -> now - 24L*60*60*1000
                        MapFilter.TimeRange.Last7d  -> now - 7L*24*60*60*1000
                        MapFilter.TimeRange.Last30d -> now - 30L*24*60*60*1000
                    }
                    pin.timestamp >= from
                }
                .filter { pin ->
                    if (f.keyword.isBlank()) true
                    else pin.title.contains(f.keyword.trim(), ignoreCase = true)
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun formatTime(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }
}