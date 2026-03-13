package com.bluearcyiji.auth

object SessionAuthState {

    @Volatile
    private var serverToken: String? = null

    fun setToken(token: String) {
        serverToken = token
    }

    fun clearToken() {
        serverToken = null
    }

    fun getToken(): String? {
        return serverToken
    }
}

