package com.example.screentimetracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ServerPinger {

    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // Ubuntu laptop's local IP address
    private const val BASE_URL = "http://172.16.1.15:3000"

    /**
     * Pings the server when a user connects/starts tracking.
     */
    suspend fun pingConnect(userName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("username", userName)
                }

                val body = jsonPayload.toString().toRequestBody(JSON)
                val request = Request.Builder()
                    .url("$BASE_URL/connect")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

}
