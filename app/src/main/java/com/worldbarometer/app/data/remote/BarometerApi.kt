package com.worldbarometer.app.data.remote

import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.data.model.BarometerData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Publiczne pliki barometer_{lens}.json (HTTPS, ~1–2 KB każdy).
 * ETag / If-Modified-Since obsługuje OkHttp Cache transparentnie (304 → serwuje
 * treść z cache, zero transferu). Aplikacja nie zna żadnego klucza API.
 */
class BarometerApi(
    private val client: OkHttpClient,
    private val json: Json,
) {

    sealed interface Result {
        data class Success(val data: BarometerData, val servedFromCache: Boolean) : Result
        data class Error(val cause: Throwable) : Result
    }

    suspend fun fetch(lensId: String = LensCatalog.DEFAULT_LENS_ID): Result = withContext(Dispatchers.IO) {
        val safeLens = LensCatalog.sanitize(lensId)
        try {
            val request = Request.Builder()
                .url(urlForLens(safeLens))
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use Result.Error(IOException("HTTP ${response.code}"))
                }
                val body = response.peekBody(MAX_BODY_BYTES).string()
                if (body.isBlank()) {
                    return@use Result.Error(IOException("Empty body"))
                }

                val data = json.decodeFromString(BarometerData.serializer(), body)
                val fromCache = response.networkResponse == null || response.networkResponse?.code == 304
                Result.Success(data, servedFromCache = fromCache)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    companion object {
        const val BASE_URL =
            "https://raw.githubusercontent.com/pb2112-netizen/barometr/main/"

        fun urlForLens(lensId: String): String =
            "${BASE_URL}barometer_${LensCatalog.sanitize(lensId)}.json"

        private const val MAX_BODY_BYTES = 256L * 1024L
    }
}
