package com.bluearcyiji.network

class ServerApiRepository(
    private val apiService: ApiService = ApiClient.service,
) {

    suspend fun getSkuList(): Result<SkuListResponse> {
        return runCatching {
            val response = apiService.getSkuList()
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                error("HTTP ${response.code()} ${response.message()} ${errorBody}".trim())
            }
            response.body() ?: error("Empty sku list response")
        }
    }

    suspend fun healthCheckText(): Result<String> {
        return runCatching {
            val response = apiService.healthCheck()
            if (!response.isSuccessful) {
                error("HTTP ${response.code()} ${response.message()}")
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
                error("HTTP ${response.code()} ${response.message()} ${errorBody}".trim())
            }
            response.body()?.token?.takeIf { it.isNotBlank() }
                ?: error("Empty token from server")
        }
    }
}

