package com.bluearcyiji.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private object AppConfig {
        private val buildConfigClass = runCatching { Class.forName("com.bluearcyiji.BuildConfig") }.getOrNull()

        val debug: Boolean = getBoolean("DEBUG", false)
        val apiBaseUrl: String = getString("API_BASE_URL", "https://example.com/")
        val apiKey: String = getString("API_KEY", "demo-key")
        val apiSecret: String = getString("API_SECRET", "demo-secret")

        private fun getBoolean(name: String, fallback: Boolean): Boolean {
            return runCatching {
                buildConfigClass?.getField(name)?.get(null) as? Boolean
            }.getOrNull() ?: fallback
        }

        private fun getString(name: String, fallback: String): String {
            return runCatching {
                buildConfigClass?.getField(name)?.get(null) as? String
            }.getOrNull() ?: fallback
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (AppConfig.debug) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        redactHeader("Authorization")
        redactHeader("X-Api-Key")
        redactHeader("X-App-Key")
        redactHeader("X-Signature")
    }

    private val authOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    // 必须先初始化：SigningInterceptor 构造/refresh 逻辑会用到它
    val authService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(AppConfig.apiBaseUrl))
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            SigningInterceptor(
                AppConfig.apiKey,
                AppConfig.apiSecret
            )
        )
        .addInterceptor(loggingInterceptor)
        .build()

    val service: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(AppConfig.apiBaseUrl))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
    }
}

