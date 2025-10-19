package com.example.rccar.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rccar.Command
import com.example.rccar.ConnectionState
import com.example.rccar.RcCarUiState

@Composable
fun DebugPanel(uiState: RcCarUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text("Debug Panel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BT Socket State:")
            Text(
                text = if (uiState.connectionState == ConnectionState.Connected) "Connected" else "Disconnected",
                color = if (uiState.connectionState == ConnectionState.Connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        // In a real app, you'd have a more robust permission status check here
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Permission Status:")
            Text("Granted (Simplified)", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Command History (Last 10):", style = MaterialTheme.typography.titleSmall)
        LazyColumn(modifier = Modifier.height(150.dp)) {
            items(uiState.commandHistory) { command ->
                Text("${command.code}: ${command.description}")
            }
        }
    }
}
