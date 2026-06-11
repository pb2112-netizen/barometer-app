package com.worldbarometer.app.di

import android.content.Context
import com.worldbarometer.app.data.local.BarometerStore
import com.worldbarometer.app.data.local.SettingsStore
import com.worldbarometer.app.data.remote.BarometerApi
import com.worldbarometer.app.data.repo.BarometerRepository
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Lekkie DI bez Hilt (decyzja MVP). Inicjowane raz w BarometerApp.onCreate().
 */
object ServiceLocator {

    @Volatile
    private var repositoryRef: BarometerRepository? = null

    @Volatile
    private var settingsStoreRef: SettingsStore? = null

    @Volatile
    private var appContextRef: Context? = null

    val repository: BarometerRepository
        get() = repositoryRef ?: error("ServiceLocator.init() nie zostało wywołane")

    val settingsStore: SettingsStore
        get() = settingsStoreRef ?: error("ServiceLocator.init() nie zostało wywołane")

    val applicationContext: Context
        get() = appContextRef ?: error("ServiceLocator.init() nie zostało wywołane")

    fun init(context: Context) {
        if (repositoryRef != null) return
        synchronized(this) {
            if (repositoryRef != null) return

            val appContext = context.applicationContext
            appContextRef = appContext

            val httpCache = Cache(File(appContext.cacheDir, "http_cache"), 5L * 1024 * 1024)
            val client = OkHttpClient.Builder()
                .cache(httpCache)
                .callTimeout(15, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }

            val api = BarometerApi(client, json)
            val store = BarometerStore(appContext)
            val settings = SettingsStore(appContext)
            repositoryRef = BarometerRepository(api, store, settings, json)
            settingsStoreRef = settings
        }
    }

    fun ensureInitialized(context: Context): BarometerRepository {
        if (repositoryRef == null) init(context)
        return repository
    }
}
