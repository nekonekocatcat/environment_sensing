package com.example.environment_sensing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.environment_sensing.data.AppDatabase
import com.example.environment_sensing.data.EnvironmentCollection
import kotlinx.coroutines.delay

@Composable
fun CollectionScreen() {
    val context = LocalContext.current
    val allEnvironments = listOf(
        "低気圧×高温レア環境",
        "暗い×うるさいレア環境",
        "うるさい×汚いレア環境",
        "薄暗い×有機ガスレア環境",
        "高温×息苦しさレア環境",
        "静かめ快適環境",
        "涼しめ明るい環境",
        "ざわざわ環境",
        /*"チェック用",*/
        "暗い静か環境"
    )

    var collected by remember { mutableStateOf<List<String>>(emptyList()) }
    var collectedEnvironments by remember { mutableStateOf<List<EnvironmentCollection>>(emptyList()) }


    LaunchedEffect(Unit) {
        val dao = AppDatabase.getInstance(context).environmentCollectionDao()
        dao.getAll().collect { result ->
            collectedEnvironments = result
        }
    }

    LaunchedEffect(collectedEnvironments) {
        if (collectedEnvironments.any { it.isNew }) {
            delay(5000)
            val dao = AppDatabase.getInstance(context).environmentCollectionDao()
            dao.clearNewFlags()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("🌱 環境コレクション", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        allEnvironments.forEach { env ->
            val match = collectedEnvironments.find { it.environmentName == env }
            val isCollected = match != null
            val isNew = match?.isNew == true

            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                when {
                    isCollected && isNew -> {
                        Text("✅ $env 🆕", fontSize = 20.sp, color = Color.Black)
                    }
                    isCollected -> {
                        Text("✅ $env", fontSize = 20.sp)
                    }
                    else -> {
                        Text("🔒 $env", fontSize = 20.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}