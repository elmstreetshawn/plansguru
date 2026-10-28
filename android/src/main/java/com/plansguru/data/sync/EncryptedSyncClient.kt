package com.plansguru.data.sync

import android.util.Log
import okhttp3.*

class EncryptedSyncClient(private val userId: String) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(serverUrl: String = "ws://10.0.2.2:8000/ws/$userId") {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("EncryptedSyncClient", "Connected to PlansGuru server relay")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("EncryptedSyncClient", "Received encrypted payload: $text")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("EncryptedSyncClient", "WebSocket connection failure", t)
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
