package com.example.sliverroad

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.sliverroad.data.LoginStatusRequest
import com.example.sliverroad.data.WorkStatusResponse
import com.example.sliverroad.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.PUT

class MainActivity : AppCompatActivity() {

    private lateinit var accessToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        accessToken = intent.getStringExtra("access_token") ?: ""

        // 3) 배송 내역 이미지 버튼
        val btnHistory = findViewById<ImageButton>(R.id.btnHistory)
        btnHistory.setOnClickListener {
            val intent = Intent(this@MainActivity, HistoryActivity::class.java).apply {
                putExtra("access_token", accessToken)
            }
            startActivity(intent)
        }

        // 4) 출근하기 버튼
        val btnStart = findViewById<ImageButton>(R.id.btnStartWork)
        btnStart.setOnClickListener {
            val statusRequest = LoginStatusRequest(is_working = true)
            accessToken = intent.getStringExtra("access_token") ?: ""
            val bearerToken = "Bearer $accessToken"

            // 1️⃣ 출근 상태 변경 요청 (PATCH)
            ApiClient.apiService.changeWorkingStatus(bearerToken, statusRequest)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Log.d("API", "출근 상태 변경 성공")

                            // 2️⃣ 출근 상태 확인 요청 (GET)
                            ApiClient.apiService.getWorkStatus(bearerToken)
                                .enqueue(object : Callback<WorkStatusResponse> {
                                    override fun onResponse(
                                        call: Call<WorkStatusResponse>,
                                        response: Response<WorkStatusResponse>
                                    ) {
                                        if (response.isSuccessful) {
                                            val status = response.body()
                                            Log.d("API", "출근 상태 확인 성공: ${status?.status_text}")

                                            // 다음 화면으로 이동
                                            val intent = Intent(this@MainActivity, CallWaitingActivity::class.java)
                                            intent.putExtra("access_token", accessToken)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Log.e("API", "출근 상태 확인 실패: ${response.code()}")
                                        }
                                    }

                                    override fun onFailure(call: Call<WorkStatusResponse>, t: Throwable) {
                                        Log.e("API", "출근 상태 확인 오류", t)
                                    }
                                })

                        } else {
                            Log.e("API", "출근 상태 변경 실패: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("API", "출근 상태 변경 오류", t)
                    }
                })
        }
        }
    }
