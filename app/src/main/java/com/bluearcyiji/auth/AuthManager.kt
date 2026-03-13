package com.bluearcyiji.auth

import android.content.Context

object AuthManager {

    @Volatile
    private var inMemoryToken: String? = null

    @Volatile
    private var initialized = false

    private lateinit var tokenStore: AuthTokenStore

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        tokenStore = AuthTokenStore(context.applicationContext)
        inMemoryToken = tokenStore.getToken()
        initialized = true
    }

    fun getToken(): String? {
        return inMemoryToken
    }

    fun saveToken(token: String) {
        if (::tokenStore.isInitialized) {
            tokenStore.saveToken(token)
        }
        inMemoryToken = token
    }

    fun clearToken() {
        if (::tokenStore.isInitialized) {
            tokenStore.clearToken()
        }
        inMemoryToken = null
    }
}

