package com.example.environment_sensing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class AppMode { Full, Simple }

//数値表示のみと環境コレクションの切り替え用
@Composable
fun ModeToggleRow(
    current: AppMode,
    onSelect: (AppMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = current == AppMode.Full,
            onClick = { onSelect(AppMode.Full) },
            label = { Text("🎮 通常") }
        )
        FilterChip(
            selected = current == AppMode.Simple,
            onClick = { onSelect(AppMode.Simple) },
            label = { Text("🧪 実験") }
        )
    }
}