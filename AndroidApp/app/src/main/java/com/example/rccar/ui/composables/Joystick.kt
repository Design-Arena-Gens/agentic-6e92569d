package com.example.rccar.ui.composables

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rccar.Command

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    onCommand: (Command) -> Unit,
    enabled: Boolean
) {
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)

    fun vibrate() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        JoystickButton(
            command = Command.FORWARD,
            onCommand = onCommand,
            onVibrate = ::vibrate,
            enabled = enabled
        )
        Row {
            JoystickButton(
                command = Command.LEFT,
                onCommand = onCommand,
                onVibrate = ::vibrate,
                enabled = enabled
            )
            Spacer(modifier = Modifier.size(80.dp))
            JoystickButton(
                command = Command.RIGHT,
                onCommand = onCommand,
                onVibrate = ::vibrate,
                enabled = enabled
            )
        }
        JoystickButton(
            command = Command.BACKWARD,
            onCommand = onCommand,
            onVibrate = ::vibrate,
            enabled = enabled
        )
    }
}

@Composable
private fun JoystickButton(
    command: Command,
    onCommand: (Command) -> Unit,
    onVibrate: () -> Unit,
    enabled: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "scale")
    val interactionSource = remember { MutableInteractionSource() }

    IconButton(
        onClick = { /* Using pointer input for press/release */ },
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
            .indication(interactionSource, androidx.compose.material.ripple.rememberRipple())
            .pointerInput(enabled) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            isPressed = true
                            onVibrate()
                            onCommand(command)
                            val press = PressInteraction.Press(it)
                            interactionSource.emit(press)
                            tryAwaitRelease()
                            interactionSource.emit(PressInteraction.Release(press))
                            isPressed = false
                            onCommand(Command.STOP)
                        }
                    }
                )
            },
        enabled = enabled
    ) {
        val icon = when (command) {
            Command.FORWARD -> Icons.Default.ArrowUpward
            Command.BACKWARD -> Icons.Default.ArrowDownward
            Command.LEFT -> Icons.Default.ArrowBack
            Command.RIGHT -> Icons.Default.ArrowForward
            else -> null
        }
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = command.description,
                modifier = Modifier.size(40.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
            )
        }
    }
}
