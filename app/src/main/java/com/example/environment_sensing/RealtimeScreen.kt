package com.example.environment_sensing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeScreen(
    viewModel: RealtimeViewModel,
    onToggleScan: (Boolean) -> Unit
) {
    val sensorData by viewModel.sensorData.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val phase by viewModel.scanUiPhase.collectAsState()
    val rareMessage by viewModel.rareMessage.collectAsState()
    val normalMessage by viewModel.normalMessage.collectAsState()
    val showRareDialog by viewModel.showRareDialog.collectAsState()
    val showNormalDialog by viewModel.showNormalDialog.collectAsState()

    // 🎉 レア環境ゲット
    if (rareMessage.isNotEmpty() && showRareDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRare() },
            confirmButton = { Button(onClick = { viewModel.dismissRare() }) { Text("OK", fontSize = 18.sp) } },
            title = { Text("🎉 レア環境ゲット！", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary) },
            text = { Text(rareMessage, fontSize = 18.sp) }
        )
    }
    // ✨ ノーマル環境ゲット
    if (normalMessage.isNotEmpty() && showNormalDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNormal() },
            confirmButton = { Button(onClick = { viewModel.dismissNormal() }) { Text("OK", fontSize = 18.sp) } },
            title = { Text("✨ ノーマル環境ゲット！", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary) },
            text = { Text(normalMessage, fontSize = 18.sp) }
        )
    }

    Scaffold(
        topBar = { SmallTopAppBar(title = { Text("環境を探してみよう！✨") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val next = !isScanning
                    onToggleScan(next)
                    viewModel.setScanning(next)
                },
                icon = {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                },
                text = { Text(if (isScanning) "探検ストップ" else "探検スタート") },
                containerColor = if (isScanning) Color(0xFFB71C1C) else MaterialTheme.colorScheme.primary
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val (titleText, descText) = when (phase) {
                RealtimeViewModel.ScanUiPhase.Idle     -> "待機中" to "探検の準備OK！"
                RealtimeViewModel.ScanUiPhase.Starting -> "起動中…" to "センサーと接続中…"
                RealtimeViewModel.ScanUiPhase.Active   -> "探検中" to "環境を監視中👀"
            }

            HeaderCardV2(titleText, descText)

            //  インジケータ
            if (phase == RealtimeViewModel.ScanUiPhase.Starting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
            }

            val sensorItems = buildSensorItems(viewModel, sensorData)
            SensorGrid(sensorItems)
        }
    }
}

// ===== ヘッダー =====
@Composable
private fun HeaderCardV2(title: String, desc: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ===== センサー表示 =====
data class SensorItem(
    val label: String,
    val valueText: String,
    val levelColor: Color
)

private fun buildSensorItems(
    vm: RealtimeViewModel,
    data: SensorData?
): List<SensorItem> = listOf(
    SensorItem("気温", if (data != null) "%.1f ℃".format(data.temperature) else "--",
        vm.levelColor(vm.tempLevel(data?.temperature))),
    SensorItem("湿度", if (data != null) "%.1f %%".format(data.humidity) else "--",
        vm.levelColor(vm.humidLevel(data?.humidity))),
    SensorItem("照度", if (data != null) "${data.light} lx" else "--",
        vm.levelColor(vm.lightLevel(data?.light))),
    SensorItem("気圧", if (data != null) "%.1f hPa".format(data.pressure) else "--",
        vm.levelColor(vm.pressureLevel(data?.pressure))),
    SensorItem("騒音", if (data != null) "%.1f dB".format(data.noise) else "--",
        vm.levelColor(vm.noiseLevel(data?.noise))),
    SensorItem("TVOC", if (data != null) "${data.tvoc} ppb" else "--",
        vm.levelColor(vm.tvocLevel(data?.tvoc))),
    SensorItem("CO₂", if (data != null) "${data.co2} ppm" else "--",
        vm.levelColor(vm.co2Level(data?.co2)))
)

@Composable
private fun SensorGrid(items: List<SensorItem>) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        items(items) { item ->
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(item.label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.valueText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(item.levelColor.copy(alpha = 0.18f))
                    )
                }
            }
        }
    }
}