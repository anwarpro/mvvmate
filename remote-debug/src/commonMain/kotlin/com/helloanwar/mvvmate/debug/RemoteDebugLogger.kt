package com.helloanwar.mvvmate.debug

import com.helloanwar.mvvmate.core.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RemoteDebugLogger(
    private val host: String = "127.0.0.1",
    private val port: Int = 8080,
    private val path: String = "/ws/mvvmate"
) : MvvMateLogger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val payloadChannel = Channel<DebugPayload>(Channel.UNLIMITED)
    
    private val client = HttpClient() {
        install(WebSockets)
    }
    
    private var session: WebSocketSession? = null

    // ── State history for time-travel (actual state objects) ──
    private data class StateSnapshot(
        val index: Int,
        val viewModelName: String,
        val state: UiState
    )

    private val stateHistory = mutableListOf<StateSnapshot>()
    private var stateIndex = 0
    private val maxSnapshots = 500

    /**
     * Callback invoked when the IDE sends an INJECT_ACTION [DebugCommand].
     *
     * SET_STATE commands are handled automatically via [MvvMate.viewModelRegistry].
     */
    var onCommandReceived: ((DebugCommand) -> Unit)? = null

    private fun handleCommand(command: DebugCommand) {
        val bridge = MvvMate.debugBridge[command.viewModelName] ?: return

        when (command.type) {
            CommandType.SET_STATE -> {
                val targetIndex = command.payload.toIntOrNull() ?: return
                val snapshot = synchronized(stateHistory) {
                    stateHistory.find { it.index == targetIndex }
                } ?: return
                bridge.restoreState(snapshot.state)
            }

            CommandType.INJECT_ACTION -> {
                bridge.injectAction(command.payload)
                onCommandReceived?.invoke(command)
            }
        }
    }

    init {
        scope.launch {
            try {
                session = client.webSocketSession {
                    url("ws://$host:$port$path")
                }
                
                // Launch a coroutine to receive IDE→app commands
                launch {
                    try {
                        val ws = session ?: return@launch
                        while (isActive) {
                            val frame = ws.incoming.receiveCatching().getOrNull() ?: break
                            if (frame is Frame.Text) {
                                try {
                                    val command = Json.decodeFromString<DebugCommand>(frame.readText())
                                    handleCommand(command)
                                } catch (_: Exception) {
                                    // Not a DebugCommand, ignore
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // WebSocket closed or error
                    }
                }

                // Keep sending whatever comes in the channel
                for (payload in payloadChannel) {
                    val jsonString = Json.encodeToString(payload)
                    session?.send(Frame.Text(jsonString))
                }
            } catch (e: Exception) {
                println("MVVMate RemoteDebugLogger failed to connect to ws://$host:$port$path : ${e.message}")
                
                for (payload in payloadChannel) {
                   // do nothing — consume to avoid memory leak
                }
            }
        }
    }

    override fun logAction(viewModelName: String, action: UiAction) {
        sendPayload(
            DebugPayload(
                type = PayloadType.ACTION,
                viewModelName = viewModelName,
                payload = action.toString(),
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    override fun logStateChange(viewModelName: String, oldState: UiState, newState: UiState) {
        // Store actual state object for time-travel
        synchronized(stateHistory) {
            stateHistory.add(StateSnapshot(stateIndex++, viewModelName, newState))
            if (stateHistory.size > maxSnapshots) {
                stateHistory.removeAt(0)
            }
        }

        sendPayload(
            DebugPayload(
                type = PayloadType.STATE_CHANGE,
                viewModelName = viewModelName,
                payload = newState.toString(),
                additionalData = oldState.toString(),
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    override fun logEffect(viewModelName: String, effect: Any) {
         sendPayload(
            DebugPayload(
                type = PayloadType.EFFECT,
                viewModelName = viewModelName,
                payload = effect.toString(),
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    override fun logError(viewModelName: String, error: Throwable, context: String) {
         sendPayload(
            DebugPayload(
                type = PayloadType.ERROR,
                viewModelName = viewModelName,
                payload = error.message ?: error.toString(),
                additionalData = context,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    override fun logNetwork(tag: String, phase: MvvMateLogger.NetworkPhase, details: String) {
         sendPayload(
            DebugPayload(
                type = PayloadType.NETWORK,
                viewModelName = "Network",
                payload = tag,
                additionalData = "${phase.name}||$details",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )
    }

    private fun sendPayload(payload: DebugPayload) {
        payloadChannel.trySend(payload)
    }
    
    fun disconnect() {
        scope.launch {
            session?.close()
            client.close()
            payloadChannel.close()
        }
    }
}
