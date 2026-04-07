package com.petmatch.mobile.data.api

import android.util.Log
import com.google.gson.Gson
import com.petmatch.mobile.data.model.SignalingMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * SignalingClient – STOMP over WebSocket (thủ công, không cần thư viện STOMP ngoài).
 *
 * Kết nối tới Spring STOMP endpoint /ws/websocket (bypass SockJS, dùng WebSocket thuần).
 * Authentication qua header STOMP CONNECT.
 *
 * Lifecycle:
 *  - Gọi [connect] khi vào CallScreen
 *  - Gọi [disconnect] khi thoát CallScreen
 */
class SignalingClient(
    private val baseWsUrl: String,   // "ws://host:port"  hoặc "wss://host:port"
    private val token: String,
    private val onSignal: (SignalingMessage) -> Unit,
    private val onConnectionEstablished: () -> Unit = {},
    private val onConnectionLost: (Throwable?) -> Unit = {}
) {
    private val TAG = "SignalingClient"
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var isConnected = false

    /** Messages queued before STOMP CONNECTED handshake completes */
    private val pendingMessages = mutableListOf<SignalingMessage>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)   // Bắt buộc 0 để không timeout khi chờ message
        .build()

    // ── STOMP frame builders ──────────────────────────────────────────────────
    private fun stompConnect(): String =
        "CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer $token\n\n\u0000"

    private fun stompSubscribe(destination: String, id: String = "sub-0"): String =
        "SUBSCRIBE\nid:$id\ndestination:$destination\n\n\u0000"

    private fun stompSend(destination: String, json: String): String {
        val bytes = json.toByteArray(Charsets.UTF_8)
        return "SEND\ndestination:$destination\ncontent-type:application/json\ncontent-length:${bytes.size}\n\n$json\u0000"
    }

    // ── Public API ────────────────────────────────────────────────────────────
    fun connect() {
        val url = "$baseWsUrl/ws/websocket"
        Log.d(TAG, "Connecting to STOMP: $url")
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, StompSocketListener())
    }

    fun sendSignal(msg: SignalingMessage) {
        if (!isConnected) {
            Log.w(TAG, "sendSignal: not connected yet – queuing $msg")
            synchronized(pendingMessages) { pendingMessages.add(msg) }
            return
        }
        transmit(msg)
    }

    private fun transmit(msg: SignalingMessage) {
        val json = gson.toJson(msg)
        val frame = stompSend("/app/chat.signal", json)
        webSocket?.send(frame) ?: Log.e(TAG, "transmit: webSocket is null")
    }

    fun disconnect() {
        try {
            webSocket?.send("DISCONNECT\n\n\u0000")
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {}
        webSocket = null
        isConnected = false
        synchronized(pendingMessages) { pendingMessages.clear() }
        Log.d(TAG, "Disconnected from STOMP")
    }

    // ── WebSocket listener ────────────────────────────────────────────────────
    private inner class StompSocketListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened – sending STOMP CONNECT")
            webSocket.send(stompConnect())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.v(TAG, "STOMP frame received: ${text.take(120)}")
            when {
                text.startsWith("CONNECTED") -> {
                    isConnected = true
                    Log.d(TAG, "STOMP CONNECTED – subscribing to signals")
                    webSocket.send(stompSubscribe("/user/queue/signals", "sub-signals"))
                    // Flush any messages queued before connection was ready
                    val queued = synchronized(pendingMessages) {
                        val copy = pendingMessages.toList()
                        pendingMessages.clear()
                        copy
                    }
                    queued.forEach { msg ->
                        Log.d(TAG, "Flushing queued signal: ${msg.type}")
                        transmit(msg)
                    }
                    onConnectionEstablished()
                }

                text.startsWith("MESSAGE") -> {
                    parseMessageFrame(text)?.let { msg ->
                        Log.d(TAG, "Signal received: type=${msg.type} from=${msg.senderId}")
                        onSignal(msg)
                    }
                }

                text.startsWith("ERROR") -> {
                    Log.e(TAG, "STOMP ERROR frame: $text")
                }

                // heart-beat or empty
                text.isBlank() || text == "\n" -> { /* ignore */ }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            isConnected = false
            onConnectionLost(t)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code – $reason")
            isConnected = false
        }
    }

    private fun parseMessageFrame(frame: String): SignalingMessage? {
        return try {
            val bodyStart = frame.indexOf("\n\n")
            if (bodyStart == -1) return null
            val body = frame.substring(bodyStart + 2).trimEnd('\u0000').trim()
            if (body.isBlank()) return null
            gson.fromJson(body, SignalingMessage::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse STOMP message: ${e.message}")
            null
        }
    }
}
