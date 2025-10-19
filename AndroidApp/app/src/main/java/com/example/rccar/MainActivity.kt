package com.example.rccar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.rccar.ui.composables.DebugPanel
import com.example.rccar.ui.composables.Joystick
import com.example.rccar.ui.composables.StatusBar
import com.example.rccar.ui.theme.RCCarTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: RcCarViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RcCarViewModel(applicationContext) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RCCarTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionHelper(
                        onPermissionsGranted = { RcCarApp(viewModel) },
                        onPermissionsDenied = { PermissionDeniedScreen() }
                    )
                }
            }
        }
    }
}

@Composable
fun RcCarApp(viewModel: RcCarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDebugPanel by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.bondedDevices.none { it.contains("HC-05", ignoreCase = true) }) {
            showPairingDialog = true
        } else {
            viewModel.connect()
        }
    }

    LaunchedEffect(uiState.connectionState) {
        if (uiState.connectionState == ConnectionState.Error) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Connection failed. Retry?",
                    actionLabel = "Retry"
                )
                viewModel.connect()
            }
        }
    }

    if (showPairingDialog) {
        PairingDialog(onDismiss = { showPairingDialog = false })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { StatusBar(uiState = uiState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 20) { // Swipe down
                            showDebugPanel = true
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uiState.isConnecting) {
                    CircularProgressIndicator()
                    Text("Connecting...", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Joystick(
                        onCommand = { viewModel.sendCommand(it) },
                        enabled = uiState.connectionState == ConnectionState.Connected
                    )
                    Spacer(modifier = Modifier.height(64.dp))
                    StopFab(
                        onClick = { viewModel.sendCommand(Command.STOP) },
                        onDoubleClick = {
                            viewModel.sendCommand(Command.STOP)
                            viewModel.sendCommand(Command.STOP)
                            viewModel.sendCommand(Command.STOP)
                        },
                        isConnected = uiState.connectionState == ConnectionState.Connected
                    )
                }
            }

            AnimatedVisibility(
                visible = showDebugPanel,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                Box(modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { showDebugPanel = false })
                }) {
                    DebugPanel(uiState = uiState)
                }
            }
        }
    }
}

@Composable
fun StopFab(onClick: () -> Unit, onDoubleClick: () -> Unit, isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleClick() },
                    onTap = { onClick() }
                )
            },
        containerColor = MaterialTheme.colorScheme.errorContainer
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "Stop",
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun PermissionDeniedScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Permissions Required", style = MaterialTheme.typography.headlineMedium)
        Text(
            "This app requires Bluetooth permissions to function. Please grant them in settings.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun PairingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("HC-05 Not Paired") },
        text = { Text("The HC-05 Bluetooth module is not paired with your device. Please go to your phone's Bluetooth settings, pair with 'HC-05', and then restart the app.") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
