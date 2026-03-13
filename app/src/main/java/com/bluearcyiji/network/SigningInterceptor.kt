package com.bluearcyiji.network

import com.bluearcyiji.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class SigningInterceptor(
    private val apiKey: String,
    private val apiSecret: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
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

        if (!serverToken.isNullOrBlank() && !request.url.encodedPath.endsWith("/auth/google_login")) {
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
