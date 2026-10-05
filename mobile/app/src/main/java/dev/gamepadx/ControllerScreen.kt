package dev.gamepadx

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val Canvas = Color(0xFF111416)
private val Panel = Color(0xFF1B2022)
private val Mint = Color(0xFF76F7C4)
private val Muted = Color(0xFF7F8B88)

@Composable
fun ControllerScreen(viewModel: ControllerViewModel) {
    val state = viewModel.state
    var showPairing by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Canvas).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(42.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GAMEPADX", color = Color.White, fontSize = 17.sp, letterSpacing = 2.sp)
                    Text("/  LOCAL PLAY", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (state.status == "Connected") "●  ${state.host}  ·  ${state.pingMs ?: "--"} ms" else "○  ${state.status}",
                        color = if (state.status == "Connected") Mint else Muted,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = { if (state.status == "Connected") viewModel.disconnect() else showPairing = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (state.status == "Connected") Panel else Mint),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(if (state.status == "Connected") "DISCONNECT" else "PAIR PC", color = if (state.status == "Connected") Color.White else Canvas, fontSize = 10.sp)
                    }
                }
            }

            Row(
                Modifier.fillMaxSize().padding(top = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TriggerPad("L2", viewModel::setTrigger)
                        HoldButton("L1", state.pressedButtons.contains("L1"), viewModel::setButton)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        DPad(state.pressedButtons, viewModel::setButton)
                        StickPad("LEFT STICK", state.leftStick, viewModel::setStick, true)
                    }
                }

                Column(
                    Modifier.width(maxWidth * 0.22f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(if (state.status == "Connected") "INPUT LINK ACTIVE" else "PAIR TO BEGIN", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HoldButton("SELECT", state.pressedButtons.contains("Select"), viewModel::setButton, compact = true, code = "Select")
                        HoldButton("START", state.pressedButtons.contains("Start"), viewModel::setButton, compact = true, code = "Start")
                    }
                    Spacer(Modifier.height(4.dp))
                    HoldButton("HOME", state.pressedButtons.contains("Home"), viewModel::setButton, compact = true, code = "Home")
                    Spacer(Modifier.height(12.dp))
                    Text("SENSITIVITY  ${(state.sensitivity * 100).roundToInt()}%", color = Muted, fontSize = 9.sp)
                    Slider(
                        value = state.sensitivity,
                        onValueChange = viewModel::setSensitivity,
                        valueRange = 0.25f..2f,
                        modifier = Modifier.width(130.dp),
                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Mint, activeTrackColor = Mint)
                    )
                    if (state.error != null) {
                        Text(state.error, color = Color(0xFFFF806E), fontSize = 9.sp, modifier = Modifier.width(160.dp))
                    }
                }

                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HoldButton("R1", state.pressedButtons.contains("R1"), viewModel::setButton)
                        TriggerPad("R2", viewModel::setTrigger)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                        FaceButtons(state.pressedButtons, viewModel::setButton)
                        StickPad("RIGHT STICK", state.rightStick, viewModel::setStick, false)
                    }
                }
            }
        }
    }

    if (showPairing) {
        PairingDialog(
            error = state.error,
            onConnect = { host, token -> viewModel.connect(host, token); showPairing = false },
            onDismiss = { showPairing = false }
        )
    }
}

@Composable
private fun PairingDialog(error: String?, onConnect: (String, String) -> Unit, onDismiss: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to PC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the PC's private Wi-Fi address and the one-time token shown in its server window.", fontSize = 13.sp)
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("PC IP or .local name") }, singleLine = true)
                OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Pairing token") }, singleLine = true)
                if (error != null) Text(error, color = Color(0xFFFF806E), fontSize = 12.sp)
            }
        },
        confirmButton = { Button(onClick = { onConnect(host, token) }) { Text("CONNECT") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
private fun HoldButton(
    label: String,
    pressed: Boolean,
    onButton: (String, Boolean) -> Unit,
    compact: Boolean = false,
    code: String = label
) {
    val size = if (compact) 54.dp else 48.dp
    Surface(
        modifier = Modifier
            .size(if (compact) 64.dp else 58.dp)
            .pointerInput(code) {
                detectTapGestures(onPress = {
                    onButton(code, true)
                    tryAwaitRelease()
                    onButton(code, false)
                })
            },
        shape = CircleShape,
        color = if (pressed) Mint else Panel,
        tonalElevation = if (pressed) 5.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (pressed) Canvas else Color.White, fontSize = if (compact) 9.sp else 12.sp)
        }
    }
}

@Composable
private fun DPad(pressed: Set<String>, onButton: (String, Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        HoldButton("U", pressed.contains("Up"), onButton, code = "Up")
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            HoldButton("L", pressed.contains("Left"), onButton, code = "Left")
            Surface(Modifier.size(44.dp), shape = CircleShape, color = Panel) { Box(contentAlignment = Alignment.Center) { Text("+", color = Muted) } }
            HoldButton("R", pressed.contains("Right"), onButton, code = "Right")
        }
        HoldButton("D", pressed.contains("Down"), onButton, code = "Down")
    }
}

@Composable
private fun FaceButtons(pressed: Set<String>, onButton: (String, Boolean) -> Unit) {
    Box(Modifier.size(width = 142.dp, height = 138.dp)) {
        HoldButton("Y", pressed.contains("Y"), onButton, ModifierOffset(48, 0))
        HoldButton("X", pressed.contains("X"), onButton, ModifierOffset(0, 42))
        HoldButton("B", pressed.contains("B"), onButton, ModifierOffset(96, 42))
        HoldButton("A", pressed.contains("A"), onButton, ModifierOffset(48, 84))
    }
}

private fun ModifierOffset(x: Int, y: Int): Modifier = androidx.compose.ui.Modifier.offset { IntOffset(x.dp.roundToPx(), y.dp.roundToPx()) }

@Composable
private fun HoldButton(label: String, pressed: Boolean, onButton: (String, Boolean) -> Unit, modifier: Modifier) {
    Box(modifier) { HoldButton(label, pressed, onButton) }
}

@Composable
private fun StickPad(label: String, value: StickVector, onStick: (Boolean, StickVector) -> Unit, left: Boolean) {
    Box(
        Modifier.size(106.dp).clip(CircleShape).background(Panel).pointerInput(left) {
            fun vectorAt(x: Float, y: Float): StickVector {
                val radius = size.width / 2f
                var dx = (x - radius) / radius
                var dy = (radius - y) / radius
                val magnitude = kotlin.math.sqrt(dx * dx + dy * dy)
                if (magnitude > 1f) {
                    dx /= magnitude
                    dy /= magnitude
                }
                return StickVector(dx, dy)
            }
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                onStick(left, vectorAt(down.position.x, down.position.y))
                down.consume()
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    onStick(left, vectorAt(change.position.x, change.position.y))
                    val stillPressed = change.pressed
                    change.consume()
                } while (stillPressed)
                onStick(left, StickVector())
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            Modifier.offset(x = (value.x * 22).dp, y = (-value.y * 22).dp).size(48.dp),
            shape = CircleShape,
            color = if (value != StickVector()) Mint else Color(0xFF37413E)
        ) {}
        Text(label, color = Muted, fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp))
    }
}

@Composable
private fun TriggerPad(label: String, onTrigger: (String, Float) -> Unit) {
    Surface(
        Modifier.size(width = 68.dp, height = 28.dp).pointerInput(label) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun amount(y: Float) = ((size.height - y) / size.height).coerceIn(0f, 1f)
                onTrigger(label, amount(down.position.y))
                down.consume()
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    onTrigger(label, amount(change.position.y))
                    val stillPressed = change.pressed
                    change.consume()
                } while (stillPressed)
                onTrigger(label, 0f)
            }
        },
        shape = RoundedCornerShape(4.dp),
        color = Panel
    ) { Box(contentAlignment = Alignment.Center) { Text(label, color = Muted, fontSize = 10.sp) } }
}
