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

data class BaseResponse<T>(
    val code: String?,
    val message: String?,
    val data: T?,
)

data class UserLoginData(
    val userId: String,
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
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

data class RecordRequest(
    val avgTime: Float,
    val maxTime: Float,
    val minTime: Float,
    val durationTime: Float,
    val totalClick: Int,
    val details: List<RecordDetail>,
)

data class RecordDetail(
    val sequence: Int,
    val clickTime: String,
)

interface ApiService {

    @GET("health")
    suspend fun healthCheck(): Response<ResponseBody>

    @GET("resources/sku/list")
    suspend fun getSkuList(): Response<BaseResponse<SkuListResponse>>

    @POST("auth/google_login")
    suspend fun login(@Body request: UserLoginRequest): Response<BaseResponse<UserLoginData>>

    @POST("record/save")
    suspend fun saveRecords(@Body request: RecordRequest): Response<BaseResponse<Any>>
}

