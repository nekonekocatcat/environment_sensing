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

@Composable
fun RealtimeScreen(
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

    // ★ ダイアログはComposableの最上位で呼ぶ（Columnの外）
    if (rareMessage.isNotEmpty() && showRareDialog) {
        AlertDialog(
            onDismissRequest = onDismissRare,
            confirmButton = {
                TextButton(onClick = onDismissRare) {
                    Text("OK")
                }
            },
            title = { Text("レア環境ゲット！") },
            text = { Text(rareMessage) }
        )
    }

    if (normalMessage.isNotEmpty() && showNormalDialog) {
        AlertDialog(
            onDismissRequest = onDismissNormal,
            confirmButton = {
                TextButton(onClick = onDismissNormal) {
                    Text("OK")
                }
            },
            title = { Text("ノーマル環境ゲット！") },
            text = { Text(normalMessage) }
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