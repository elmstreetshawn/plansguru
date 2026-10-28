package com.plansguru.data.sync

import android.content.Context
import android.util.Log
import okhttp3.*

class EncryptedSyncClient(
    private val context: Context,
    private val sessionId: String = "default_session"
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(onMessageReceived: (String) -> Unit = {}) {
        val prefs = context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE)
        val httpUrl = prefs.getString("server_url", "https://few-jars-wear.loca.lt")?.trimEnd('/') ?: "https://few-jars-wear.loca.lt"
        val wsUrl = httpUrl.replace("https://", "wss://").replace("http://", "ws://") + "/ws/session/$sessionId"

        Log.d("EncryptedSyncClient", "Connecting WebSocket relay to: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Bypass-Tunnel-Remainder", "true")
            .addHeader("ngrok-skip-browser-warning", "true")
            .addHeader("User-Agent", "PlansGuruApp/1.0")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("EncryptedSyncClient", "Connected to PlansGuru server relay ($wsUrl)")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("EncryptedSyncClient", "Received encrypted payload: $text")
                onMessageReceived(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("EncryptedSyncClient", "WebSocket connection failure on $wsUrl", t)
            }
        })
    }

    fun sendEncryptedPayload(jsonPayload: String) {
        webSocket?.send(jsonPayload)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
    }
}
