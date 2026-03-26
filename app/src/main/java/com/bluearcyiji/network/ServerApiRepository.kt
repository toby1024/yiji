package com.bluearcyiji.network

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ApiHttpException(
    val statusCode: Int,
    override val message: String,
) : RuntimeException(message)

data class AuthSession(
    val userId: String,
    val token: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val premiumInfo: String,
    val premiumExpireTimeEpochSeconds: Long,
)

data class UserPremiumStatus(
    val premiumInfo: String,
    val premiumExpireTimeEpochSeconds: Long,
)

class ServerApiRepository(
    private val apiService: ApiService = ApiClient.service,
) {

    private fun isBusinessSuccess(code: Int?): Boolean {
        return code == 200
    }

    private fun <T> unwrapOrThrow(baseResponse: BaseResponse<T>?): T {
        val payload = baseResponse ?: error("Empty response body")
        if (!isBusinessSuccess(payload.code)) {
            error("Business error ${payload.code}: ${payload.message.orEmpty()}")
        }
        return payload.data ?: error("Empty response data")
    }

    private fun throwHttpError(code: Int, message: String, errorBody: String) {
        val detail = "HTTP $code $message ${errorBody}".trim()
        throw ApiHttpException(statusCode = code, message = detail)
    }

    private fun normalizeEpochSeconds(raw: Long): Long {
        if (raw <= 0L) return 0L
        // Backward-compatible normalization: accepts both seconds and millis.
        return if (raw > 9_999_999_999L) raw / 1000L else raw
    }

    suspend fun getSkuList(): Result<SkuListResponse> {
        return runCatching {
            val response = apiService.getSkuList()
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                throwHttpError(response.code(), response.message(), errorBody)
            }
            unwrapOrThrow(response.body())
        }
    }

    suspend fun healthCheckText(): Result<String> {
        return runCatching {
            val response = apiService.healthCheck()
            if (!response.isSuccessful) {
                throwHttpError(response.code(), response.message(), "")
            }
            response.body()?.string().orEmpty()
        }
    }

    private fun UserLoginData.toAuthSession(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): AuthSession {
        return AuthSession(
            userId = userId,
            token = token,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = nowEpochSeconds + expiresIn,
            premiumInfo = premiumInfo.orEmpty(),
            premiumExpireTimeEpochSeconds = normalizeEpochSeconds(premiumExpireTime),
        )
    }

    suspend fun loginWithGoogleIdToken(idToken: String, email: String): Result<AuthSession> {
        return runCatching {
            val response = apiService.login(
                UserLoginRequest(
                    idToken = idToken,
                    email = email,
                )
            )
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                throwHttpError(response.code(), response.message(), errorBody)
            }
            val loginData = unwrapOrThrow(response.body())
            loginData.token.takeIf { it.isNotBlank() } ?: error("Empty token from server")
            loginData.refreshToken.takeIf { it.isNotBlank() } ?: error("Empty refresh token from server")
            loginData.toAuthSession()
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): Result<AuthSession> {
        return runCatching {
            val response = apiService.refreshToken(RefreshTokenRequest(refreshToken = refreshToken))
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                throwHttpError(response.code(), response.message(), errorBody)
            }
            val loginData = unwrapOrThrow(response.body())
            loginData.token.takeIf { it.isNotBlank() } ?: error("Empty token from refresh API")
            loginData.refreshToken.takeIf { it.isNotBlank() } ?: error("Empty refresh token from refresh API")
            loginData.toAuthSession()
        }
    }

    suspend fun saveRecords(record: RecordRequest): Result<Unit> {
        return runCatching {
            val response = apiService.saveRecords(record)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                throwHttpError(response.code(), response.message(), errorBody)
            }
            val payload = response.body() ?: return@runCatching
            if (!isBusinessSuccess(payload.code)) {
                if (payload.code == 402) {
                    throw ApiHttpException(
                        statusCode = 402,
                        message = "Business error 402: ${payload.message.orEmpty()}",
                    )
                }
                if (payload.code == 403) {
                    throw ApiHttpException(
                        statusCode = 403,
                        message = "Business error 403: ${payload.message.orEmpty()}",
                    )
                }
                error("Business error ${payload.code}: ${payload.message.orEmpty()}")
            }
            Unit
        }
    }

    private fun parseDateTimeToEpochSeconds(raw: String?): Long {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return 0L
        value.toLongOrNull()?.let { return normalizeEpochSeconds(it) }
        val userTimeParsers = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        )
        userTimeParsers.forEach { parser ->
            runCatching { parser.parse(value) }
                .getOrNull()
                ?.let { date -> return date.time / 1000L }
        }
        return 0L
    }

    suspend fun fetchUserPremiumStatus(): Result<UserPremiumStatus> {
        return runCatching {
            val response = apiService.getUserInfo()
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                throwHttpError(response.code(), response.message(), errorBody)
            }

            val payload = response.body() ?: error("Empty response body")
            if (payload.code != 200) {
                error("Business error ${payload.code}: ${payload.message.orEmpty()}")
            }

            val userInfo = payload.data?.userInfo ?: error("Empty userInfo")
            UserPremiumStatus(
                premiumInfo = userInfo.premiumInfo.orEmpty(),
                premiumExpireTimeEpochSeconds = parseDateTimeToEpochSeconds(userInfo.premiumExpireTime),
            )
        }
    }

    suspend fun getRecordHistory(pageNum: Int, pageSize: Int): Result<RecordHistoryPage> {
        return runCatching {
            val response = apiService.getRecordHistory(RecordHistoryRequest(pageNum, pageSize))
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                throwHttpError(response.code(), response.message(), errorBody)
            }
            unwrapOrThrow(response.body())
        }
    }
}
