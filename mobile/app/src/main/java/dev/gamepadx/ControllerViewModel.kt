package dev.gamepadx

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val SERVER_PORT = 8765

data class StickVector(val x: Float = 0f, val y: Float = 0f)

data class ControllerUiState(
    val status: String = "Disconnected",
    val host: String = "",
    val pingMs: Long? = null,
    val leftStick: StickVector = StickVector(),
    val rightStick: StickVector = StickVector(),
    val pressedButtons: Set<String> = emptySet(),
    val sensitivity: Float = 1f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val error: String? = null
)

class ControllerViewModel : ViewModel() {
    private val client = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var shouldReconnect = false
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private var inputJob: Job? = null
    private var pairingToken = ""

    var state by mutableStateOf(ControllerUiState())
        private set

    fun connect(hostInput: String, tokenInput: String) {
        val host = hostInput.trim()
        val token = tokenInput.trim()
        if (!isLocalHost(host)) {
            state = state.copy(error = "Use a private LAN IPv4 address or a .local host name.")
            return
        }
        if (!token.matches(Regex("[a-fA-F0-9]{32}"))) {
            state = state.copy(error = "Pairing token must be the 32-character token shown by the PC server.")
            return
        }
        shouldReconnect = true
        pairingToken = token
        state = state.copy(status = "Connecting", host = host, error = null, pingMs = null)
        openSocket()
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        pingJob?.cancel()
        inputJob?.cancel()
        webSocket?.close(1000, "Disconnected by user")
        webSocket = null
        state = neutralState(status = "Disconnected", pingMs = null)
    }

    fun setButton(code: String, pressed: Boolean) {
        val buttons = if (pressed) state.pressedButtons + code else state.pressedButtons - code
        state = state.copy(pressedButtons = buttons)
        sendInput()
    }

    fun setStick(left: Boolean, vector: StickVector) {
        state = if (left) state.copy(leftStick = vector) else state.copy(rightStick = vector)
        if (inputJob?.isActive != true) {
            inputJob = viewModelScope.launch {
                delay(16)
                sendInput()
            }
        }
    }

    fun setTrigger(name: String, value: Float) {
        state = if (name == "L2") state.copy(leftTrigger = value.coerceIn(0f, 1f))
        else state.copy(rightTrigger = value.coerceIn(0f, 1f))
        sendInput()
    }

    fun setSensitivity(value: Float) {
        state = state.copy(sensitivity = value.coerceIn(0.25f, 2f))
        sendInput()
    }

    private fun openSocket() {
        if (!shouldReconnect) return
        state = state.copy(status = "Connecting", error = null)
        val request = Request.Builder()
            .url("ws://${state.host}:$SERVER_PORT/ws?token=$pairingToken")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(socket: WebSocket, response: Response) {
                if (socket !== webSocket) return
                reconnectAttempt = 0
                state = state.copy(status = "Connected", error = null)
                startPingLoop()
                sendInput()
            }

            override fun onMessage(socket: WebSocket, text: String) {
                if (socket !== webSocket) return
                runCatching {
                    val message = JSONObject(text)
                    if (message.optString("type") == "pong") {
                        val sentAt = message.optLong("timestamp")
                        state = state.copy(pingMs = (SystemClock.elapsedRealtime() - sentAt).coerceAtLeast(0))
                    }
                }
            }

            override fun onMessage(socket: WebSocket, bytes: ByteString) = Unit

            override fun onClosed(socket: WebSocket, code: Int, reason: String) {
                handleLoss(socket, "Connection closed")
            }

            override fun onFailure(socket: WebSocket, error: Throwable, response: Response?) {
                val rejected = response?.code == 401
                handleLoss(socket, if (rejected) "Pairing token rejected" else "Connection lost")
                if (rejected) shouldReconnect = false
            }
        })
    }

    private fun handleLoss(socket: WebSocket, reason: String) {
        if (socket !== webSocket) return
        webSocket = null
        pingJob?.cancel()
        inputJob?.cancel()
        state = neutralState(status = "Disconnected", pingMs = null, error = reason)
        if (shouldReconnect) {
            val waitMs = (500L shl reconnectAttempt.coerceAtMost(3)).coerceAtMost(4000L)
            reconnectAttempt++
            reconnectJob?.cancel()
            reconnectJob = viewModelScope.launch {
                delay(waitMs)
                openSocket()
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val sentAt = SystemClock.elapsedRealtime()
                webSocket?.send(JSONObject().put("type", "ping").put("timestamp", sentAt).toString())
            }
        }
    }

    private fun sendInput() {
        val socket = webSocket ?: return
        if (state.status != "Connected") return
        val factor = state.sensitivity
        val buttons = JSONArray()
        state.pressedButtons.sorted().forEach(buttons::put)
        val packet = JSONObject()
            .put("type", "input")
            .put("timestamp", SystemClock.elapsedRealtime())
            .put("buttons", buttons)
            .put("leftStick", stickJson(state.leftStick, factor))
            .put("rightStick", stickJson(state.rightStick, factor))
            .put("triggers", JSONObject().put("L2", state.leftTrigger).put("R2", state.rightTrigger))
        socket.send(packet.toString())
    }

    private fun stickJson(stick: StickVector, sensitivity: Float) = JSONObject()
        .put("x", (stick.x * sensitivity).coerceIn(-1f, 1f))
        .put("y", (stick.y * sensitivity).coerceIn(-1f, 1f))

    private fun neutralState(status: String, pingMs: Long? = null, error: String? = null) = state.copy(
        status = status,
        pingMs = pingMs,
        error = error,
        leftStick = StickVector(),
        rightStick = StickVector(),
        leftTrigger = 0f,
        rightTrigger = 0f,
        pressedButtons = emptySet()
    )

    private fun isLocalHost(host: String): Boolean {
        if (host.endsWith(".local", ignoreCase = true)) return true
        val octets = host.split('.')
        if (octets.size != 4) return false
        val values = octets.map { it.toIntOrNull() ?: return false }
        if (values.any { it !in 0..255 }) return false
        return values[0] == 10 || values[0] == 192 && values[1] == 168 ||
            values[0] == 172 && values[1] in 16..31
    }

    override fun onCleared() {
        disconnect()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
