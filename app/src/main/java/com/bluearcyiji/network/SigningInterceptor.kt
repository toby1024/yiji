package com.bluearcyiji.network

import com.bluearcyiji.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.runBlocking
import java.util.UUID

class SigningInterceptor(
    private val apiKey: String,
    private val apiSecret: String,
) : Interceptor {

    private val repository = ServerApiRepository()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        val path = request.url.encodedPath
        val skipAuth = path.endsWith("/auth/google_login") || path.endsWith("/auth/refresh_token")

        if (!skipAuth && AuthManager.shouldRefreshToken()) {
            runBlocking {
                val refreshToken = AuthManager.getRefreshToken()
                if (!refreshToken.isNullOrBlank()) {
                    repository.refreshAccessToken(refreshToken)
                        .onSuccess { session ->
                            AuthManager.saveSession(
                                token = session.token,
                                refreshToken = session.refreshToken,
                                expiresAtEpochSeconds = session.expiresAtEpochSeconds,
                            )
                        }
                }
            }
        }

        val serverToken = AuthManager.getToken()
        val timestamp = System.currentTimeMillis() / 1000

        if (request.url.encodedPath.startsWith("/resources/")) {
            val signature = SignatureGenerator.hmacSha256Base64(
                secret = apiSecret,
                canonicalString = "$apiKey:$timestamp",
            )
            val skuRequest = requestBuilder
                .header("X-App-Key", apiKey)
                .header("X-Timestamp", timestamp.toString())
                .header("X-Signature", signature)
                .build()
            return chain.proceed(skuRequest)
        }

        if (!serverToken.isNullOrBlank() && !skipAuth) {
            requestBuilder.header("Authorization", "Bearer $serverToken")
        }
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val canonicalString = SignatureGenerator.createCanonicalString(
            request = request,
            timestampSeconds = timestamp,
            nonce = nonce,
        )
        val signature = SignatureGenerator.hmacSha256Base64(apiSecret, canonicalString)

        val signedRequest = requestBuilder
            .header("X-App-Key", apiKey)
            .header("X-Timestamp", timestamp.toString())
            .header("X-Nonce", nonce)
            .header("X-Signature", signature)
            .build()

        return chain.proceed(signedRequest)
    }
}
