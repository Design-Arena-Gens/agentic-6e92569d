package com.example.rccar

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

private const val TAG = "RcCarViewModel"
private const val HC05_NAME = "HC-05"
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

data class RcCarUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val lastCommand: Command = Command.STOP,
    val commandHistory: List<Command> = emptyList(),
    val bondedDevices: List<String> = emptyList(),
    val isConnecting: Boolean = false
)

enum class ConnectionState {
    Connected, Disconnected, Connecting, Error
}

enum class Command(val code: Char, val description: String) {
    FORWARD('F', "↑ Forward"),
    BACKWARD('B', "↓ Backward"),
    LEFT('L', "← Left"),
    RIGHT('R', "→ Right"),
    STOP('S', "■ Stop");

    companion object {
        fun fromChar(c: Char): Command? = entries.find { it.code == c }
    }
}

@SuppressLint("MissingPermission")
class RcCarViewModel(context: Context) : ViewModel() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val _uiState = MutableStateFlow(RcCarUiState())
    val uiState: StateFlow<RcCarUiState> = _uiState.asStateFlow()

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var connectJob: Job? = null

    init {
        getBondedDevices()
    }

    fun connect() {
        if (uiState.value.isConnecting || uiState.value.connectionState == ConnectionState.Connected) return

        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, connectionState = ConnectionState.Connecting) }
            val device = findHc05Device()
            if (device == null) {
                Log.e(TAG, "HC-05 device not found in bonded devices.")
                _uiState.update { it.copy(isConnecting = false, connectionState = ConnectionState.Error) }
                return@launch
            }

            var attempt = 0
            while (attempt < 3 && bluetoothSocket == null) {
                try {
                    withContext(Dispatchers.IO) {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                        bluetoothSocket?.connect()
                        outputStream = bluetoothSocket?.outputStream
                    }
                    _uiState.update { it.copy(isConnecting = false, connectionState = ConnectionState.Connected) }
                    Log.i(TAG, "Connected to HC-05")
                    return@launch
                } catch (e: IOException) {
                    Log.e(TAG, "Connection attempt ${attempt + 1} failed", e)
                    bluetoothSocket = null
                    outputStream = null
                    attempt++
                    if (attempt < 3) delay(5000)
                }
            }

            if (bluetoothSocket == null) {
                _uiState.update { it.copy(isConnecting = false, connectionState = ConnectionState.Error) }
            }
        }
    }

    fun disconnect() {
        sendCommand(Command.STOP)
        connectJob?.cancel()
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing bluetooth socket", e)
        } finally {
            outputStream = null
            bluetoothSocket = null
            _uiState.update { it.copy(connectionState = ConnectionState.Disconnected, isConnecting = false) }
            Log.i(TAG, "Disconnected from HC-05")
        }
    }

    fun sendCommand(command: Command) {
        if (uiState.value.connectionState != ConnectionState.Connected) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(command.code.code)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        val newHistory = (listOf(command) + it.commandHistory).take(10)
                        it.copy(lastCommand = command, commandHistory = newHistory)
                    }
                }
                Log.d(TAG, "Sent command: ${command.code}")
            } catch (e: IOException) {
                Log.e(TAG, "Error sending command", e)
                withContext(Dispatchers.Main) {
                    disconnect()
                    _uiState.update { it.copy(connectionState = ConnectionState.Error) }
                }
            }
        }
    }

    private fun findHc05Device(): BluetoothDevice? {
        return bluetoothAdapter?.bondedDevices?.find { it.name.equals(HC05_NAME, ignoreCase = true) }
    }

    private fun getBondedDevices() {
        val devices = bluetoothAdapter?.bondedDevices?.map { it.name } ?: emptyList()
        _uiState.update { it.copy(bondedDevices = devices) }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
