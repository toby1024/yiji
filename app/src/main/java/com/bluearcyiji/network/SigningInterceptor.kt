package com.bluearcyiji.network

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class SigningInterceptor(
    private val apiKey: String,
    private val apiSecret: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val timestamp = System.currentTimeMillis() / 1000
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val bodyHash = SignatureGenerator.bodySha256Hex(request)
        val canonicalString = SignatureGenerator.createCanonicalString(
            request = request,
            timestampSeconds = timestamp,
            nonce = nonce,
            bodyHashHex = bodyHash,
        )
        val signature = SignatureGenerator.hmacSha256Base64(apiSecret, canonicalString)

        val signedRequest = request.newBuilder()
            .header("X-Api-Key", apiKey)
            .header("X-Timestamp", timestamp.toString())
            .header("X-Nonce", nonce)
            .header("X-Signature", signature)
            .build()

        return chain.proceed(signedRequest)
    }
}
