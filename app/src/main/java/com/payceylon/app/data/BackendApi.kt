package com.payceylon.app.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BackendApi {
    /**
     * Host for local development.
     * - Android Emulator: "10.0.2.2"
     * - Physical Android phone: Use your computer's local Wi-Fi IP (e.g. "172.20.10.2")
     *   or run `adb reverse tcp:8000 tcp:8000` & `adb reverse tcp:9099 tcp:9099` and use "127.0.0.1".
     */
    const val HOST = "172.20.10.2"


    private val baseUrl = if (com.payceylon.app.BuildConfig.DEBUG) {
        "http://$HOST:8000/v1"
    } else {
        "https://api.payceylon.lk/v1"
    }

    suspend fun verifyPhone(idToken: String): JSONObject =
        request("POST", "/auth/verify", JSONObject().put("idToken", idToken), authenticated = false)

    suspend fun get(path: String): JSONObject = request("GET", path)

    suspend fun post(path: String, body: JSONObject, idempotencyKey: String? = null): JSONObject =
        request("POST", path, body, idempotencyKey = idempotencyKey)

    suspend fun patch(path: String, body: JSONObject): JSONObject = request("PATCH", path, body)

    suspend fun delete(path: String) {
        request("DELETE", path)
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
        idempotencyKey: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        val token = if (authenticated) {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                ?: error("Your sign-in has expired. Please sign in again.")
        } else {
            null
        }
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            idempotencyKey?.let { connection.setRequestProperty("Idempotency-Key", it) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                val response = connection.errorStream?.bufferedReader()?.use { it.readText() }
                val error = response?.let { runCatching { JSONObject(it).getJSONObject("error").getString("message") }.getOrNull() }
                error(error ?: "The server could not complete your request (HTTP $status).")
            }
            val response = connection.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }
}
