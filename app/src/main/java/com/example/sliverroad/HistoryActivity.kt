package com.example.sliverroad

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sliverroad.api.ApiClient
import com.example.sliverroad.api.ApiClient.apiService
import com.example.sliverroad.data.AppDatabase
import com.example.sliverroad.data.DeliveryHistory
import com.example.sliverroad.data.DeliveryHistoryItem
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        tvDate    = findViewById(R.id.tvDate)

        adapter = HistoryAdapter(emptyList())
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        val today = intent.getStringExtra("date") ?: "오늘 날짜"
        tvDate.text = today

        val token = intent.getStringExtra("access_token") ?: ""

        apiService.getDeliveryHistory("Bearer $token")
            .enqueue(object : Callback<List<DeliveryHistoryItem>> {
                override fun onResponse(
                    call: Call<List<DeliveryHistoryItem>>,
                    response: Response<List<DeliveryHistoryItem>>
                ) {
                    if (response.isSuccessful) {
                        val historyList = response.body() ?: emptyList()
                        adapter.submitList(historyList)
                    } else {
                        Log.e("API", "❌ 히스토리 조회 실패: ${response.code()} / ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(
                    call: Call<List<DeliveryHistoryItem>>,
                    t: Throwable
                ) {
                    Log.e("API", "❌ 히스토리 네트워크 오류", t)
                }
            })}}
