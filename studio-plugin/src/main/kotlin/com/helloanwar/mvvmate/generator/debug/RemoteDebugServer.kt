package com.helloanwar.mvvmate.generator.debug

import com.helloanwar.mvvmate.debug.DebugCommand
import com.helloanwar.mvvmate.debug.DebugPayload
import com.helloanwar.mvvmate.debug.PayloadType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress
import javax.swing.SwingUtilities

/**
 * Indexed wrapper around a [DebugPayload] for time-travel history.
 */
data class IndexedPayload(
    val index: Int,
    val payload: DebugPayload
)

object RemoteDebugServer {

    private var server: DebugWebSocketServer? = null
    
    var onUpdate: (() -> Unit)? = null
    
    private val _logs = mutableListOf<DebugPayload>()
    private val _latestStates = mutableMapOf<String, String>()
    
    /** State history for time-travel debugging (ring buffer, max 500). */
    private val _stateHistory = mutableListOf<IndexedPayload>()
    private var _stateIndex = 0
    private val maxHistory = 500

    /** Unique action names encountered for suggestions: ViewModel -> (ActionType -> FullPayload) */
    private val _actionSuggestions = mutableMapOf<String, MutableMap<String, String>>()
    val actionSuggestions: Map<String, Map<String, String>> get() = synchronized(this) { 
        _actionSuggestions.mapValues { it.value.toMap() } 
    }
    
    /** Connected WebSocket clients for broadcasting commands back to the app. */
    private val _connectedClients = mutableSetOf<WebSocket>()

    val logs: List<DebugPayload> get() = synchronized(this) { _logs.toList() }
    val latestStates: Map<String, String> get() = synchronized(this) { _latestStates.toMap() }
    val stateHistory: List<IndexedPayload> get() = synchronized(this) { _stateHistory.toList() }

    fun start(port: Int = 8080) {
        if (server != null) return
        server = DebugWebSocketServer(InetSocketAddress(port)).apply {
             start()
        }
    }

    private fun handlePayload(payload: DebugPayload) {
        synchronized(this) {
            _logs.add(payload)
            if (_logs.size > 1000) {
                _logs.removeAt(0)
            }
            if (payload.type == PayloadType.ACTION) {
                val vmMap = _actionSuggestions.getOrPut(payload.viewModelName) { mutableMapOf() }
                // Extract action type (e.g. "FullNameChanged") from payload "FullNameChanged(value=...)"
                val type = payload.payload.substringBefore('(').substringBefore('.')
                vmMap[type] = payload.payload
            }
            if (payload.type == PayloadType.STATE_CHANGE) {
                _latestStates[payload.viewModelName] = payload.payload
                
                // Record state snapshot for time-travel
                val indexed = IndexedPayload(index = _stateIndex++, payload = payload)
                _stateHistory.add(indexed)
                if (_stateHistory.size > 500) {
                    _stateHistory.removeAt(0)
                }
            }
        }
        SwingUtilities.invokeLater {
            onUpdate?.invoke()
        }
    }

    /**
     * Send a [DebugCommand] to all connected app clients.
     */
    fun sendCommand(command: DebugCommand) {
        val json = Json.encodeToString(command)
        synchronized(this) {
            _connectedClients.forEach { ws ->
                try {
                    if (ws.isOpen) {
                        ws.send(json)
                    }
                } catch (_: Exception) {
                    // Client may have disconnected
                }
            }
        }
    }

    fun stop() {
        server?.stop(1000)
        server = null
        synchronized(this) {
            _logs.clear()
            _latestStates.clear()
            _stateHistory.clear()
            _stateIndex = 0
            _actionSuggestions.clear()
            _connectedClients.clear()
        }
        SwingUtilities.invokeLater {
            onUpdate?.invoke()
        }
    }
    
    fun clearLogs() {
        synchronized(this) {
            _logs.clear()
            _latestStates.clear()
            _stateHistory.clear()
            _stateIndex = 0
            _actionSuggestions.clear()
        }
        SwingUtilities.invokeLater {
            onUpdate?.invoke()
        }
    }
    
    private class DebugWebSocketServer(address: InetSocketAddress) : WebSocketServer(address) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            conn?.let { 
                synchronized(RemoteDebugServer) {
                    _connectedClients.add(it)
                }
            }
        }
        
        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            conn?.let {
                synchronized(RemoteDebugServer) {
                    _connectedClients.remove(it)
                }
            }
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            if (message == null) return
            try {
                val payload = Json.decodeFromString<DebugPayload>(message)
                handlePayload(payload)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            ex?.printStackTrace()
        }

        override fun onStart() {
            println("RemoteDebugServer started successfully on port ${address.port}")
        }
    }
}
