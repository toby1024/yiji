package com.bluearcyiji.auth

import android.content.Context

object AuthManager {

    @Volatile
    private var inMemoryToken: String? = null
    @Volatile
    private var inMemoryRefreshToken: String? = null
    @Volatile
    private var expiresAtEpochSeconds: Long = 0L

    @Volatile
    private var initialized = false

    private lateinit var tokenStore: AuthTokenStore

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        tokenStore = AuthTokenStore(context.applicationContext)
        inMemoryToken = tokenStore.getToken()
        inMemoryRefreshToken = tokenStore.getRefreshToken()
        expiresAtEpochSeconds = tokenStore.getExpiresAtEpochSeconds()
        initialized = true
    }

    fun getToken(): String? {
        return inMemoryToken
    }

    fun saveSession(token: String, refreshToken: String, expiresAtEpochSeconds: Long) {
        if (::tokenStore.isInitialized) {
            tokenStore.saveSession(token, refreshToken, expiresAtEpochSeconds)
        }
        inMemoryToken = token
        inMemoryRefreshToken = refreshToken
        this.expiresAtEpochSeconds = expiresAtEpochSeconds
    }

    fun getRefreshToken(): String? {
        return inMemoryRefreshToken
    }

    fun shouldRefreshToken(bufferSeconds: Long = 60L): Boolean {
        val token = inMemoryToken
        if (token.isNullOrBlank()) return false
        if (expiresAtEpochSeconds <= 0L) return false
        val nowSeconds = System.currentTimeMillis() / 1000
        return nowSeconds + bufferSeconds >= expiresAtEpochSeconds
    }

    fun clearToken() {
        if (::tokenStore.isInitialized) {
            tokenStore.clearToken()
        }
        inMemoryToken = null
        inMemoryRefreshToken = null
        expiresAtEpochSeconds = 0L
    }
}

