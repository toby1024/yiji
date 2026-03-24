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
    private var inMemoryUserId: String? = null
    @Volatile
    private var inMemoryDisplayName: String? = null
    @Volatile
    private var inMemoryPremiumInfo: String? = null
    @Volatile
    private var premiumExpireTimeEpochSeconds: Long = 0L

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
        inMemoryUserId = tokenStore.getUserId()
        inMemoryDisplayName = tokenStore.getDisplayName()
        inMemoryPremiumInfo = tokenStore.getPremiumInfo()
        premiumExpireTimeEpochSeconds = tokenStore.getPremiumExpireTimeEpochSeconds()
        initialized = true
    }

    fun getToken(): String? {
        return inMemoryToken
    }

    fun saveSession(
        token: String,
        refreshToken: String,
        expiresAtEpochSeconds: Long,
        userId: String,
        displayName: String?,
        premiumInfo: String,
        premiumExpireTimeEpochSeconds: Long,
    ) {
        if (::tokenStore.isInitialized) {
            tokenStore.saveSession(
                token = token,
                refreshToken = refreshToken,
                expiresAtEpochSeconds = expiresAtEpochSeconds,
                userId = userId,
                displayName = displayName,
                premiumInfo = premiumInfo,
                premiumExpireTimeEpochSeconds = premiumExpireTimeEpochSeconds,
            )
        }
        inMemoryToken = token
        inMemoryRefreshToken = refreshToken
        this.expiresAtEpochSeconds = expiresAtEpochSeconds
        inMemoryUserId = userId
        inMemoryDisplayName = displayName
        inMemoryPremiumInfo = premiumInfo
        this.premiumExpireTimeEpochSeconds = premiumExpireTimeEpochSeconds
    }

    fun getRefreshToken(): String? {
        return inMemoryRefreshToken
    }

    fun getUserId(): String? {
        return inMemoryUserId
    }

    fun getDisplayName(): String? {
        return inMemoryDisplayName
    }

    fun getPremiumInfo(): String? {
        return inMemoryPremiumInfo
    }

    fun getPremiumExpireTimeEpochSeconds(): Long {
        return premiumExpireTimeEpochSeconds
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
        inMemoryUserId = null
        inMemoryDisplayName = null
        inMemoryPremiumInfo = null
        premiumExpireTimeEpochSeconds = 0L
    }
}

