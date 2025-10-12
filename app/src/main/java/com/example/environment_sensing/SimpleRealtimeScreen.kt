package com.example.environment_sensing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleRealtimeScreen(
    viewModel: RealtimeViewModel,
    isScanning: Boolean,
    onToggleScan: (Boolean) -> Unit,
    onBackToFull: () -> Unit
) {
    val data by viewModel.sensorData.collectAsState()

    val rareMessage by viewModel.rareMessage.collectAsState()
    val normalMessage by viewModel.normalMessage.collectAsState()
    val showRareDialog by viewModel.showRareDialog.collectAsState()
    val showNormalDialog by viewModel.showNormalDialog.collectAsState()


    // 🎉 レア環境ゲット
    if (rareMessage.isNotEmpty() && showRareDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRare() },
            confirmButton = { Button(onClick = { viewModel.dismissRare() }) { Text("OK") } },
            title = { Text("🎉 レア環境ゲット！", color = MaterialTheme.colorScheme.primary) },
            text  = { Text(rareMessage) }
        )
    }
    // ✨ ノーマル環境ゲット
    if (normalMessage.isNotEmpty() && showNormalDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNormal() },
            confirmButton = { Button(onClick = { viewModel.dismissNormal() }) { Text("OK") } },
            title = { Text("✨ ノーマル環境ゲット！", color = MaterialTheme.colorScheme.primary) },
            text  = { Text(normalMessage) }
        )
    }

    Scaffold(
        topBar = { SmallTopAppBar(title = { Text("🧪 実験モード") }) },
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
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModeToggleRow(
                current = AppMode.Simple,
                onSelect = { if (it == AppMode.Full) onBackToFull() }
            )

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
            ) {
                Text(
                    "この画面は評価実験用に数値のみを表示します。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }

            val titleStyle = MaterialTheme.typography.titleMedium
            val valueStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)

            @Composable
            fun Line(label: String, value: String) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = titleStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = valueStyle)
                }
            }

            Line("温度", if (data != null) "%.1f ℃".format(data!!.temperature) else "--")
            Line("湿度", if (data != null) "%.1f %%".format(data!!.humidity) else "--")
            Line("気圧", if (data != null) "%.1f hPa".format(data!!.pressure) else "--")
            Line("照度", if (data != null) "${data!!.light} lx" else "--")
            Line("騒音", if (data != null) "%.1f dB".format(data!!.noise) else "--")
            Line("TVOC", if (data != null) "${data!!.tvoc} ppb" else "--")
            Line("CO₂", if (data != null) "${data!!.co2} ppm" else "--")
        }
    }
}