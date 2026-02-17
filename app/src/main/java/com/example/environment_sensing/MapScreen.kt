package com.example.environment_sensing

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

private const val PIN_SIZE_DP = 50f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val pins by vm.pins.collectAsState()
    val filter by vm.filter.collectAsState()
    val scope = rememberCoroutineScope()

    val hasFine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val myLocationEnabled = hasFine || hasCoarse

    val fallback = pins.lastOrNull()?.let { LatLng(it.lat, it.lon) }
        ?: LatLng(35.681236, 139.767125) // 東京駅
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fallback, 13f)
    }

    val locationProvider = remember { LocationProvider(context.applicationContext) }
    var movedToMyLocation by remember { mutableStateOf(false) }
    LaunchedEffect(myLocationEnabled) {
        if (myLocationEnabled && !movedToMyLocation) {
            locationProvider.currentOrNull()?.let {
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory
                        .newLatLngZoom(LatLng(it.lat, it.lon), 16f)
                )
                movedToMyLocation = true
            }
        }
    }

    var sheetOpen by remember { mutableStateOf(false) }

    var mapReady by remember { mutableStateOf(false) }
    var rareIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var normalIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    Box(Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false
            ),
            properties = MapProperties(isMyLocationEnabled = myLocationEnabled)
        ) {
            MapEffect {
                if (!mapReady) {
                    mapReady = true
                    // ピン色を固定
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
                    val icon = if (pin.isRare) rareIcon else normalIcon
                    Marker(
                        state = MarkerState(LatLng(pin.lat, pin.lon)),
                        title = pin.title,
                        snippet = pin.snippet,
                        icon = icon,
                        anchor = Offset(0.5f, 1f)
                    )
                }
            }
        }

        FilterSummaryBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            filter = filter,
            count = pins.size,
            onClick = { sheetOpen = true }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { shareMapSnapshot(context, pins) },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text("シェア") }
            )
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        locationProvider.currentOrNull()?.let {
                            cameraPositionState.animate(
                                com.google.android.gms.maps.CameraUpdateFactory
                                    .newLatLngZoom(LatLng(it.lat, it.lon), 16f)
                            )
                        }
                    }
                }
            ) { Icon(Icons.Filled.MyLocation, contentDescription = "現在地") }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }) {
            FilterSheet(
                filter = filter,
                onKindChange = { kind -> vm.updateFilter { cur -> cur.copy(kind = kind) } },
                onToggleTier = { tier ->
                    vm.updateFilter { cur ->
                        val set = cur.tiers.toMutableSet()
                        if (set.contains(tier)) set.remove(tier) else set.add(tier)
                        cur.copy(tiers = set)
                    }
                },
                onTimeRangeChange = { range -> vm.updateFilter { cur -> cur.copy(timeRange = range) } },
                onKeywordChange = { kw -> vm.updateFilter { cur -> cur.copy(keyword = kw) } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    filter: MapFilter,
    onKindChange: (MapFilter.Kind) -> Unit,
    onToggleTier: (Int) -> Unit,
    onTimeRangeChange: (MapFilter.TimeRange) -> Unit,
    onKeywordChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("フィルタ", style = MaterialTheme.typography.titleMedium)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SegChip("すべて", filter.kind == MapFilter.Kind.All) { onKindChange(MapFilter.Kind.All) }
            SegChip("レア", filter.kind == MapFilter.Kind.RareOnly) { onKindChange(MapFilter.Kind.RareOnly) }
            SegChip("ノーマル", filter.kind == MapFilter.Kind.NormalOnly) { onKindChange(MapFilter.Kind.NormalOnly) }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TierChip(1, 1 in filter.tiers, onToggleTier)
            TierChip(2, 2 in filter.tiers, onToggleTier)
            TierChip(3, 3 in filter.tiers, onToggleTier)
            TierChip(99, 99 in filter.tiers, onToggleTier, "Ultra")
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SegChip("24h", filter.timeRange == MapFilter.TimeRange.Last24h) { onTimeRangeChange(MapFilter.TimeRange.Last24h) }
            SegChip("7日", filter.timeRange == MapFilter.TimeRange.Last7d) { onTimeRangeChange(MapFilter.TimeRange.Last7d) }
            SegChip("30日", filter.timeRange == MapFilter.TimeRange.Last30d) { onTimeRangeChange(MapFilter.TimeRange.Last30d) }
            SegChip("すべて", filter.timeRange == MapFilter.TimeRange.All) { onTimeRangeChange(MapFilter.TimeRange.All) }
        }

        OutlinedTextField(
            value = filter.keyword,
            onValueChange = onKeywordChange,
            singleLine = true,
            label = { Text("キーワード") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FilterSummaryBar(
    modifier: Modifier = Modifier,
    filter: MapFilter,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val kind = when (filter.kind) {
                MapFilter.Kind.All -> "すべて"
                MapFilter.Kind.RareOnly -> "レア"
                MapFilter.Kind.NormalOnly -> "ノーマル"
            }
            AssistChip(onClick = onClick, label = { Text(kind) })

            if (filter.tiers.isNotEmpty()) {
                AssistChip(onClick = onClick, label = { Text("Tier: " + filter.tiers.sorted().joinToString()) })
            }

            val range = when (filter.timeRange) {
                MapFilter.TimeRange.Last24h -> "24h"
                MapFilter.TimeRange.Last7d -> "7日"
                MapFilter.TimeRange.Last30d -> "30日"
                MapFilter.TimeRange.All -> "すべて"
            }
            AssistChip(onClick = onClick, label = { Text(range) })

            Spacer(Modifier.weight(1f))
            Text("表示: $count 件", style = MaterialTheme.typography.labelMedium)
            Text("フィルタ", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SegChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
private fun TierChip(tier: Int, selected: Boolean, onToggleTier: (Int) -> Unit, label: String? = null) {
    val (text, tint) = when (tier) {
        1 -> "Tier1" to MaterialTheme.colorScheme.primary
        2 -> "Tier2" to Color(0xFFE91E63)
        3 -> "Tier3" to Color(0xFFFFC107)
        99 -> (label ?: "Ultra") to Color(0xFF7E57C2)
        else -> "Tier$tier" to MaterialTheme.colorScheme.primary
    }
    AssistChip(
        onClick = { onToggleTier(tier) },
        label = { Text(text) },
        leadingIcon = if (selected) { { Icon(Icons.Filled.Check, null, tint = tint) } } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) tint.copy(0.18f) else MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun TimeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = if (selected) { { Icon(Icons.Filled.Check, null) } } else null
    )
}

//共有用テキスト
fun shareMapSnapshot(context: Context, pins: List<MapPin>) {
    val rareCount = pins.count { it.isRare }
    val normalCount = pins.size - rareCount
    val latest = pins.maxByOrNull { it.timestamp }?.timestamp
    val latestStr = latest?.let {
        java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(it))
    } ?: "-"

    val text = buildString {
        appendLine("環境コレクション：マップ戦果📍")
        appendLine("・合計: ${pins.size}件（レア: $rareCount / ノーマル: $normalCount）")
        appendLine("・最新: $latestStr")
        append("#環境コレクション #カンコレ")
    }

    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(shareIntent, "共有"))
}