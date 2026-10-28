package com.plansguru.data.sync

import android.content.Context
import android.util.Log
import com.plansguru.data.crypto.ChannelCryptoHelper
import com.plansguru.data.model.ProtocolMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*

class EncryptedSyncClient(
    private val context: Context,
    private val sessionId: String = "default_session"
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

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

    fun listenOnFriendChannels(friendsList: List<String>, onProtocolMessageReceived: (ProtocolMessage) -> Unit) {
        val prefs = context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE)
        val httpUrl = prefs.getString("server_url", "https://few-jars-wear.loca.lt")?.trimEnd('/') ?: "https://few-jars-wear.loca.lt"

        for (friend in friendsList) {
            val channelTopic = ChannelCryptoHelper.computeChannelTopic("local_user", friend)
            val wsUrl = httpUrl.replace("https://", "wss://").replace("http://", "ws://") + "/ws/session/$channelTopic"

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Bypass-Tunnel-Remainder", "true")
                .addHeader("ngrok-skip-browser-warning", "true")
                .addHeader("User-Agent", "PlansGuruApp/1.0")
                .build()

            client.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val msg = json.decodeFromString<ProtocolMessage>(text)
                        if (msg.senderId != "local_user") {
                            onProtocolMessageReceived(msg)
                        }
                    } catch (e: Exception) {
                        Log.w("EncryptedSyncClient", "Error parsing channel message: ${e.message}")
                    }
                }
            })
        }
    }

    fun broadcastPlanRequest(groupTitle: String, participants: List<String>) {
        val prefs = context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE)
        val httpUrl = prefs.getString("server_url", "https://few-jars-wear.loca.lt")?.trimEnd('/') ?: "https://few-jars-wear.loca.lt"

        for (p in participants) {
            if (p == "local_user") continue
            val channelTopic = ChannelCryptoHelper.computeChannelTopic("local_user", p)
            val wsUrl = httpUrl.replace("https://", "wss://").replace("http://", "ws://") + "/ws/session/$channelTopic"

            val message = ProtocolMessage(
                type = "PLAN_REQUEST",
                senderId = "local_user",
                sessionId = channelTopic,
                payload = json.encodeToString(mapOf("groupTitle" to groupTitle, "participants" to participants.joinToString(","))),
                signature = "sig_local_user",
                timestamp = System.currentTimeMillis()
            )

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Bypass-Tunnel-Remainder", "true")
                .addHeader("ngrok-skip-browser-warning", "true")
                .addHeader("User-Agent", "PlansGuruApp/1.0")
                .build()

            client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    ws.send(json.encodeToString(message))
                    Log.d("EncryptedSyncClient", "Broadcasted PLAN_REQUEST to $p on $channelTopic")
                }
            })
        }
    }

    fun sendEncryptedPayload(jsonPayload: String) {
        webSocket?.send(jsonPayload)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
    }
}
