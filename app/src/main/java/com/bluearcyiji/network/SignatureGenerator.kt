package com.bluearcyiji.network

import android.util.Base64
import okhttp3.Request
import okio.Buffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SignatureGenerator {

    fun generateNonce(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun createCanonicalString(
        request: Request,
        timestampSeconds: Long,
        nonce: String,
    ): String {
        val method = request.method.uppercase(Locale.US)
        val path = request.url.encodedPath
        val query = canonicalizeEncodedQuery(request.url.encodedQuery)
        val bodyHash = bodySha256Hex(request)
        
        return listOf(
            method,
            path,
            query,
            timestampSeconds.toString(),
            nonce,
            bodyHash,
        ).joinToString("\n")
    }

    private fun canonicalizeEncodedQuery(encodedQuery: String?): String {
        if (encodedQuery.isNullOrBlank()) return ""

        val normalizedPairs = encodedQuery
            .split("&")
            .filter { it.isNotEmpty() }
            .map { part ->
                val separatorIndex = part.indexOf('=')
                if (separatorIndex >= 0) {
                    part.substring(0, separatorIndex) to part.substring(separatorIndex + 1)
                } else {
                    part to ""
                }
            }
            .sortedWith(compareBy<Pair<String, String>>({ it.first }, { it.second }))

        return normalizedPairs.joinToString("&") { (key, value) ->
            if (value.isEmpty()) key else "$key=$value"
        }
    }

    fun bodySha256Hex(request: Request): String {
        val body = request.body ?: return sha256Hex(ByteArray(0))
        val buffer = Buffer()
        body.writeTo(buffer)
        return sha256Hex(buffer.readByteArray())
    }

    fun hmacSha256Base64(secret: String, canonicalString: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(canonicalString.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun sha256Hex(value: ByteArray): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value)
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }
}
