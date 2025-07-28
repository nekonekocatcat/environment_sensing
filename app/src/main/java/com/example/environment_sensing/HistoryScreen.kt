package com.example.environment_sensing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.RareEnvironmentLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val rareDao = remember { AppDatabase.getInstance(context).rareEnvironmentLogDao() }
    val normalDao = remember { AppDatabase.getInstance(context).normalEnvironmentLogDao() }

    val rareLogsFlow = remember { rareDao.getAllLogs() }
    val normalLogsFlow = remember { normalDao.getAllLogs() }

    val rareLogs by rareLogsFlow.collectAsState(initial = emptyList())
    val normalLogs by normalLogsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📘 環境ゲット履歴", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (rareLogs.isNotEmpty()) {
            Text("🌟 レア環境", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(rareLogs) { log ->
                    HistoryItem("レア", log.environmentName, log.timestamp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (normalLogs.isNotEmpty()) {
            Text("✅ ノーマル環境", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(normalLogs) { log ->
                    HistoryItem("ノーマル", log.environmentName, log.timestamp)
                }
            }
        }
    }
}

@Composable
fun HistoryItem(type: String, name: String, timestamp: Long) {
    val formattedTime = remember(timestamp) {
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    Text(text = "・[$formattedTime] $name ($type)")
    Spacer(modifier = Modifier.height(8.dp))
}