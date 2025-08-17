package com.example.environment_sensing


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.environment_sensing.SensorData
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun RealtimeScreen(
    viewModel: RealtimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    sensorData: SensorData?,
    rareMessage: String,
    normalMessage: String,
    onStartScan: () -> Unit,
    showRareDialog: Boolean,
    showNormalDialog: Boolean,
    onDismissRare: () -> Unit,
    onDismissNormal: () -> Unit
) {
    val scrollState = rememberScrollState()

    val sensorData by viewModel.sensorData.collectAsState()
    val rareMessage by viewModel.rareMessage.collectAsState()
    val normalMessage by viewModel.normalMessage.collectAsState()
    val showRareDialog by viewModel.showRareDialog.collectAsState()
    val showNormalDialog by viewModel.showNormalDialog.collectAsState()

    if (rareMessage.isNotEmpty() && showRareDialog) {
        AlertDialog(
            onDismissRequest = onDismissRare,
            confirmButton = {
                Button(onClick = onDismissRare) {
                    Text("OK", fontSize = 18.sp)
                }
            },
            title = {
                Text("🎉 レア環境ゲット！", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
            },
            text = {
                Text(
                    rareMessage,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        )
    }

    if (normalMessage.isNotEmpty() && showNormalDialog) {
        AlertDialog(
            onDismissRequest = onDismissNormal,
            confirmButton = {
                Button(onClick = onDismissNormal) {
                    Text("OK", fontSize = 18.sp)
                }
            },
            title = { Text("✨ノーマル環境ゲット！", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary) },
            text = {
                Text(
                    normalMessage,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Button(onClick = onStartScan) {
            Text("BLEスキャン開始")
        }

        Spacer(modifier = Modifier.height(16.dp))

        sensorData?.let { data ->
            Text("🌡 気温: ${"%.1f".format(data.temperature)}℃", fontSize = 24.sp)
            Text("💧 湿度: ${"%.1f".format(data.humidity)}%", fontSize = 24.sp)
            Text("💡 照度: ${data.light} lx", fontSize = 24.sp)
            Text("📈 気圧: ${"%.1f".format(data.pressure)} hPa", fontSize = 24.sp)
            Text("🔊 騒音: ${"%.1f".format(data.noise)} dB", fontSize = 24.sp)
            Text("🌫 TVOC: ${data.tvoc} ppb", fontSize = 24.sp)
            Text("🌬 CO2: ${data.co2} ppm", fontSize = 24.sp)
        }
    }
}