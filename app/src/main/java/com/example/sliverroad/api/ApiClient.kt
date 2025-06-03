package com.example.sliverroad.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://largeredjade.site"

    // 1) OkHttpClient에 타임아웃 설정을 추가
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // 연결 타임아웃
        .readTimeout(60, TimeUnit.SECONDS)    // 읽기 타임아웃
        .writeTimeout(60, TimeUnit.SECONDS)   // 쓰기 타임아웃
        .build()

    // 2) 위에서 만든 okHttpClient를 Retrofit에 .client(...)로 설정
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())

        .build()

    // 3) 만들어진 Retrofit 인스턴스에서 ApiService를 가져오기
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
