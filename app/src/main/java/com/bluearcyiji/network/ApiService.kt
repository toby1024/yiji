package com.bluearcyiji.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {

    @GET("health")
    suspend fun healthCheck(): Response<ResponseBody>
}

