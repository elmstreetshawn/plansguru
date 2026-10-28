package com.plansguru.data.api

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class RegisterUserRequest(
    val userId: String,
    val publicKey: String,
    val displayName: String
)

@Serializable
data class RegisterUserResponse(
    val status: String,
    val userId: String,
    val message: String? = null
)

class ServerApiClient(private val context: Context) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private fun getServerUrl(): String {
        val prefs = context.getSharedPreferences("plansguru_prefs", Context.MODE_PRIVATE)
        return prefs.getString("server_url", "https://smart-lizards-nail.loca.lt")?.trimEnd('/')
            ?: "https://smart-lizards-nail.loca.lt"
    }

    suspend fun registerUser(userId: String, publicKey: String, displayName: String): Result<RegisterUserResponse> {
        return try {
            val request = RegisterUserRequest(userId, publicKey, displayName)
            val requestBody = json.encodeToString(request)
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("${getServerUrl()}/api/register")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Bypass-Tunnel-Reminder", "true")
                .addHeader("Bypass-Tunnel-Remainder", "true")
                .addHeader("ngrok-skip-browser-warning", "true")
                .addHeader("User-Agent", "PlansGuruApp/1.0")
                .build()

            val response = client.newCall(httpRequest).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: "{}"
                val parsedResponse = json.decodeFromString<RegisterUserResponse>(body)
                Log.d("ServerApiClient", "Registration successful: $userId")
                Result.success(parsedResponse)
            } else {
                val error = response.body?.string() ?: "Unknown error"
                Log.e("ServerApiClient", "Registration failed: $error")
                Result.failure(Exception("Registration failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ServerApiClient", "Registration error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserPublicKey(userId: String): Result<String> {
        return try {
            val httpRequest = Request.Builder()
                .url("${getServerUrl()}/api/users/$userId/publickey")
                .get()
                .addHeader("Bypass-Tunnel-Reminder", "true")
                .addHeader("Bypass-Tunnel-Remainder", "true")
                .addHeader("ngrok-skip-browser-warning", "true")
                .addHeader("User-Agent", "PlansGuruApp/1.0")
                .build()

            val response = client.newCall(httpRequest).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Log.d("ServerApiClient", "Retrieved public key for: $userId")
                Result.success(body.trim().removeSurrounding("\""))
            } else {
                Log.e("ServerApiClient", "Failed to get public key: ${response.code}")
                Result.failure(Exception("Failed to get public key"))
            }
        } catch (e: Exception) {
            Log.e("ServerApiClient", "Error getting public key: ${e.message}", e)
            Result.failure(e)
        }
    }
}
