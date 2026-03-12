package com.bluearcyiji.network

class ServerApiRepository(
    private val apiService: ApiService = ApiClient.service,
) {

    suspend fun healthCheckText(): Result<String> {
        return runCatching {
            val response = apiService.healthCheck()
            if (!response.isSuccessful) {
                error("HTTP ${response.code()} ${response.message()}")
            }
            response.body()?.string().orEmpty()
        }
    }
}

