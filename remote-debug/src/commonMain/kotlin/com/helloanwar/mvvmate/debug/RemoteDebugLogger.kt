package com.helloanwar.mvvmate.debug

import com.helloanwar.mvvmate.core.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
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

    init {
        scope.launch {
            try {
                session = client.webSocketSession {
                    url("ws://$host:$port$path")
                }
                
                // Keep sending whatever comes in the channel
                for (payload in payloadChannel) {
                    val jsonString = Json.encodeToString(payload)
                    session?.send(Frame.Text(jsonString))
                }
            } catch (e: Exception) {
                // Silently fail or fallback to print logger if connection fails
                println("MVVMate RemoteDebugLogger failed to connect to ws://$host:$port$path : ${e.message}")
                
                // Still consume channel to avoid memory leaks
                for (payload in payloadChannel) {
                   // do nothing
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
