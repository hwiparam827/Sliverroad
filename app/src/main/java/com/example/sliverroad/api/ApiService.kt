package com.example.sliverroad.api
import com.example.sliverroad.data.LoginStatusRequest
import retrofit2.Call
import retrofit2.http.*
import com.example.sliverroad.data.LoginRequest
import com.example.sliverroad.data.LoginResponse
import com.example.sliverroad.data.WorkStatusResponse
import com.example.sliverroad.data.CallStatusResponse
import com.example.sliverroad.data.LocationRequest
import com.example.sliverroad.data.LocationResponse
import com.example.sliverroad.data.CallRequestDetailResponse

import okhttp3.RequestBody
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


    /** 접수 상세 조회 */
    @GET("/api/delivery/delivery-detail/{id}/")
    fun getRequestDetail(
        @Header("Authorization") bearerToken: String,
        @Path("id") requestId: String
    ): Call<CallRequestDetailResponse>


}

