package com.example.environment_sensing.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AutoDismissAlertDialog(
    title: String,
    message: String,
    timeoutSeconds: Int = 8,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    var sec by remember(title, message) { mutableStateOf(timeoutSeconds) }

    LaunchedEffect(title, message) {
        sec = timeoutSeconds
        while (sec > 0) {
            delay(1000)
            sec--
        }
        onConfirm()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                title,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                message,
                fontSize = 18.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK ($sec)", fontSize = 16.sp)
            }
        }
    )
}