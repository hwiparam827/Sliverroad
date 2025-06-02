package com.example.sliverroad.api

import com.example.sliverroad.data.LoginStatusRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import com.example.sliverroad.data.LoginRequest
import com.example.sliverroad.data.LoginResponse
import com.example.sliverroad.data.WorkStatusResponse
import com.example.sliverroad.data.CallStatusResponse
import com.example.sliverroad.data.LocationRequest
import com.example.sliverroad.data.LocationResponse
import com.example.sliverroad.data.CallRequestDetailResponse
import com.example.sliverroad.data.AcceptCallRequest
import com.example.sliverroad.data.DeclineCallRequest
import com.example.sliverroad.data.DeliveryHistoryItem
import com.example.sliverroad.model.FindRouteResponse
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody


data class RouteRequestBody(
    @SerializedName("destination_type")
    val destinationType: String  // "pickup" 또는 "delivery"
)

interface ApiService {
    @POST("/api/user/login/")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @PATCH("/api/delivery/status/")
    fun changeWorkingStatus(
        @Header("Authorization") token: String,
        @Body request: LoginStatusRequest
    ): Call<Void>

    @PATCH("/api/delivery/call-reception/")
    fun toggleCallReceiveStatus(
        @Header("Authorization") token: String,
        @Body body: RequestBody
    ): Call<CallStatusResponse>


    @GET("/api/delivery/status/")
    fun getWorkStatus(
        @Header("Authorization") token: String
    ): Call<WorkStatusResponse>

    // 배달원 위치 최신화
    @PATCH("api/delivery/location/")
    fun updateLocation(
        @Header("Authorization") token: String,
        @Body body: LocationRequest
    ): Call<LocationResponse>


    @GET("/api/delivery/delivery-detail/{pk}/")
    suspend fun getDeliveryDetail(
        @Header("Authorization") bearerToken: String,
        @Path("pk") pk: String
    ): Response<CallRequestDetailResponse>


    // 배달 수락
    @PATCH("/api/delivery/assignment/{assignment_id}/accept/")
    fun acceptcall(
        @Header("Authorization") token: String,
        @Path("assignment_id") assignmentId: Int,
        @Body body: RequestBody
    ): Call<AcceptCallRequest>

    // 배달 거절
    @PATCH("/api/delivery/assignment/{assignment_id}/decline/")
    fun declineCall(
        @Header("Authorization") token: String,
        @Path("assignment_id") assignmentId: Int,
        @Body body: RequestBody
    ): Call<DeclineCallRequest>


    @POST("/api/delivery_route/find-route/{request_id}/")
    fun findRoute(
        @Header("Authorization") authHeader: String,
        @Path("request_id") requestId: String,
        @Body body: Map<String, String>
    ): Call<FindRouteResponse>


    @Multipart
    @PATCH("/api/delivery/delivery-complete/{assignment_id}/")
    fun completeDeliveryWithPhoto(
        @Header("Authorization") token: String,
        @Path("assignment_id") assignmentId: Int,
        @Part photo: MultipartBody.Part
    ): Call<CompleteDeliveryResponse>


    @Multipart
    @PATCH("/api/delivery/item-receive/{assignment_id}/")
    fun completeRecieptWithPhoto(
        @Header("Authorization") token: String,
        @Path("assignment_id") assignmentId: Int,
        @Part photo: MultipartBody.Part
    ): Call<CompleteRecieptResponse>

    data class CompleteDeliveryResponse(
        val id: Int
    )
    data class CompleteRecieptResponse(
        val id: Int
    )

    @GET("/api/delivery/delivery-history/")
    fun getDeliveryHistory(
        @Header("Authorization") token: String
    ): Call<List<DeliveryHistoryItem>>
}








