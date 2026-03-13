package com.bluearcyiji.network

import okhttp3.ResponseBody
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

data class UserLoginRequest(
    val idToken: String,
    val email: String,
)

data class UserLoginResponse(
    val token: String,
)

data class SkuItem(
    val skuId: String,
    val skuName: String,
    val skuPrice: Int,
    val quantity: Int,
)

data class SkuListPayload(
    @SerializedName("one_time")
    val oneTime: List<SkuItem>,
    val subscription: List<SkuItem>,
)

data class SkuListResponse(
    val skuList: SkuListPayload,
)

interface ApiService {

    @GET("health")
    suspend fun healthCheck(): Response<ResponseBody>

    @GET("resources/sku/list")
    suspend fun getSkuList(): Response<SkuListResponse>

    @POST("auth/google_login")
    suspend fun login(@Body request: UserLoginRequest): Response<UserLoginResponse>
}

