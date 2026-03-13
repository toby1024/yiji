package com.bluearcyiji.network

class ApiHttpException(
    val statusCode: Int,
    override val message: String,
) : RuntimeException(message)

class ServerApiRepository(
    private val apiService: ApiService = ApiClient.service,
) {

    private fun isBusinessSuccess(code: Int): Boolean {
        return code == 200
    }

    private fun <T> unwrapOrThrow(baseResponse: BaseResponse<T>?): T {
        val payload = baseResponse ?: error("Empty response body")
        if (!isBusinessSuccess(payload.code)) {
            error("Business error ${payload.code}: ${payload.message}")
        }
        return payload.data ?: error("Empty response data")
    }

    private fun throwHttpError(code: Int, message: String, errorBody: String) {
        val detail = "HTTP $code $message ${errorBody}".trim()
        throw ApiHttpException(statusCode = code, message = detail)
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

    suspend fun loginWithGoogleIdToken(idToken: String, email: String): Result<String> {
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
            unwrapOrThrow(response.body()).token.takeIf { it.isNotBlank() }
                ?: error("Empty token from server")
        }
    }

    suspend fun saveRecords(records: List<RecordRequest>): Result<Unit> {
        return runCatching {
            val response = apiService.saveRecords(records)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                throwHttpError(response.code(), response.message(), errorBody)
            }
            val payload = response.body() ?: error("Empty response body")
            if (!isBusinessSuccess(payload.code)) {
                error("Business error ${payload.code}: ${payload.message}")
            }
            Unit
        }
    }
}
