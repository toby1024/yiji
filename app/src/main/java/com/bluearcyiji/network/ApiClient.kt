package com.bluearcyiji.network

import com.bluearcyiji.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private object AppConfig {
        val debug: Boolean = BuildConfig.DEBUG
        val apiBaseUrl: String = BuildConfig.API_BASE_URL
        val apiKey: String = BuildConfig.API_KEY
        val apiSecret: String = BuildConfig.API_SECRET
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
        .addInterceptor { chain ->
            // authOkHttpClient is used by SigningInterceptor's internal repository to refresh
            // tokens. It cannot reuse SigningInterceptor (circular init), so we apply a
            // lightweight signing-headers-only interceptor here to ensure X-App-Key,
            // X-Timestamp, X-Nonce and X-Signature are always present on auth requests.
            val request = chain.request()
            val timestamp = System.currentTimeMillis() / 1000
            val nonce = SignatureGenerator.generateNonce()
            val canonicalString = SignatureGenerator.createCanonicalString(
                request = request,
                timestampSeconds = timestamp,
                nonce = nonce,
            )
            val signature = SignatureGenerator.hmacSha256Base64(AppConfig.apiSecret, canonicalString)
            chain.proceed(
                request.newBuilder()
                    .header("X-App-Key", AppConfig.apiKey)
                    .header("X-Timestamp", timestamp.toString())
                    .header("X-Nonce", nonce)
                    .header("X-Signature", signature)
                    .build()
            )
        }
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

