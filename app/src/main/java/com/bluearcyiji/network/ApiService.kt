package com.bluearcyiji.network

import androidx.annotation.Keep
import okhttp3.ResponseBody
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

@Keep
data class UserLoginRequest(
    @SerializedName("idToken")
    val idToken: String,
    @SerializedName("email")
    val email: String,
)

@Keep
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
)

@Keep
data class BaseResponse<T>(
    val code: String?,
    val message: String?,
    val data: T?,
)

@Keep
data class UserLoginData(
    val userId: String,
    val token: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Keep
data class SkuItem(
    val skuId: String,
    val skuName: String,
    val skuPrice: Int,
    val quantity: Int,
    val isPopular: Boolean,
)

@Keep
data class SkuListPayload(
    @SerializedName("one_time")
    val oneTime: List<SkuItem>,
    val subscription: List<SkuItem>,
)

@Keep
data class SkuListResponse(
    val skuList: SkuListPayload,
)

@Keep
data class RecordRequest(
    val avgTime: Float,
    val maxTime: Float,
    val minTime: Float,
    val durationTime: Float,
    val totalClick: Int,
    val details: List<RecordDetail>,
)

@Keep
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

    @POST("auth/refresh_token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<BaseResponse<UserLoginData>>

    @POST("record/save")
    suspend fun saveRecords(@Body request: RecordRequest): Response<BaseResponse<Any>>
}
