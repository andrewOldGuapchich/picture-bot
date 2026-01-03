package com.andrew.tg.service

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class HttpClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    fun get(url: String, headers: Map<String, String> = emptyMap()): Triple<Int, String?, String?> {
        return try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                val statusCode = response.code
                val body = response.body?.string()

                when {
                    response.isSuccessful -> Triple(statusCode, body, null)
                    else -> Triple(statusCode, null, "HTTP Error: $statusCode")
                }
            }
        } catch (e: SocketTimeoutException) {
            Triple(-1, null, "Timeout: ${e.message}")
        } catch (e: IOException) {
            Triple(-1, null, "Network error: ${e.message}")
        } catch (e: Exception) {
            Triple(-1, null, "Unexpected error: ${e.message}")
        }
    }
}