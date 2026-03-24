package com.bluearcyiji.auth

import android.content.Context

class AuthTokenStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(
        token: String,
        refreshToken: String,
        expiresAtEpochSeconds: Long,
        userId: String,
        premiumInfo: String,
        premiumExpireTimeEpochSeconds: Long,
    ) {
        preferences.edit()
            .putString(KEY_SERVER_TOKEN, token)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT_EPOCH_SECONDS, expiresAtEpochSeconds)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_PREMIUM_INFO, premiumInfo)
            .putLong(KEY_PREMIUM_EXPIRE_EPOCH_SECONDS, premiumExpireTimeEpochSeconds)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString(KEY_SERVER_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }

    fun getExpiresAtEpochSeconds(): Long {
        return preferences.getLong(KEY_EXPIRES_AT_EPOCH_SECONDS, 0L)
    }

    fun getUserId(): String? {
        return preferences.getString(KEY_USER_ID, null)
    }

    fun getPremiumInfo(): String? {
        return preferences.getString(KEY_PREMIUM_INFO, null)
    }

    fun getPremiumExpireTimeEpochSeconds(): Long {
        return preferences.getLong(KEY_PREMIUM_EXPIRE_EPOCH_SECONDS, 0L)
    }

    fun clearToken() {
        preferences.edit()
            .remove(KEY_SERVER_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT_EPOCH_SECONDS)
            .remove(KEY_USER_ID)
            .remove(KEY_PREMIUM_INFO)
            .remove(KEY_PREMIUM_EXPIRE_EPOCH_SECONDS)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_SERVER_TOKEN = "server_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT_EPOCH_SECONDS = "expires_at_epoch_seconds"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PREMIUM_INFO = "premium_info"
        private const val KEY_PREMIUM_EXPIRE_EPOCH_SECONDS = "premium_expire_epoch_seconds"
    }
}

