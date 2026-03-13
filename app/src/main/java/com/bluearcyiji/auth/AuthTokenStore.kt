package com.bluearcyiji.auth

import android.content.Context

class AuthTokenStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        preferences.edit().putString(KEY_SERVER_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return preferences.getString(KEY_SERVER_TOKEN, null)
    }

    fun clearToken() {
        preferences.edit().remove(KEY_SERVER_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_SERVER_TOKEN = "server_token"
    }
}

