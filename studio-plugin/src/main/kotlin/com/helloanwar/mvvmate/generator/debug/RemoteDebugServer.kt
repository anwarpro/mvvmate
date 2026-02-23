package com.helloanwar.mvvmate.generator.debug

import com.helloanwar.mvvmate.debug.DebugPayload
import kotlinx.serialization.json.Json
import org.java_websocket.server.WebSocketServer
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress
import javax.swing.SwingUtilities

object RemoteDebugServer {

    private var server: DebugWebSocketServer? = null
    
    var onUpdate: (() -> Unit)? = null
    
    private val _logs = mutableListOf<DebugPayload>()
    private val _latestStates = mutableMapOf<String, String>()

    val logs: List<DebugPayload> get() = synchronized(this) { _logs.toList() }
    val latestStates: Map<String, String> get() = synchronized(this) { _latestStates.toMap() }

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
            if (payload.type == com.helloanwar.mvvmate.debug.PayloadType.STATE_CHANGE) {
                _latestStates[payload.viewModelName] = payload.payload
            }
        }
        SwingUtilities.invokeLater {
            onUpdate?.invoke()
        }
    }

    fun stop() {
        server?.stop(1000)
        server = null
        synchronized(this) {
            _logs.clear()
            _latestStates.clear()
        }
        SwingUtilities.invokeLater {
            onUpdate?.invoke()
        }
    }
    
    fun clearLogs() {
        synchronized(this) {
            _logs.clear()
            _latestStates.clear()
        }
        SwingUtilities.invokeLater {
            onUpdate?.invoke()
        }
    }
    
    private class DebugWebSocketServer(address: InetSocketAddress) : WebSocketServer(address) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {}
        
        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {}

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
