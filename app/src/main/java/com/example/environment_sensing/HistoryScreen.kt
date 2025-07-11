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
    val dao = remember {
        AppDatabase.getInstance(context).rareEnvironmentLogDao()
    }

    val logsFlow = remember { dao.getAllLogs() }
    val logs by logsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("レア環境ゲット履歴", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(logs) { log ->
                val time = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                Text("・[$time] ${log.environmentName}")
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}