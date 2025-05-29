package com.example.sliverroad

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sliverroad.data.AppDatabase
import com.example.sliverroad.data.DeliveryHistory
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    // DB, 뷰, 어댑터 선언
    private lateinit var db: AppDatabase
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 1) DB 인스턴스 초기화
        db = AppDatabase.getInstance(this)

        // 2) findViewById 로 뷰 바인딩
        rvHistory = findViewById(R.id.rvHistory)
        tvDate    = findViewById(R.id.tvDate)

        // 3) RecyclerView 세팅
        adapter = HistoryAdapter(emptyList())
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter

        // 4) 날짜 가져와서 표시
        val today = intent.getStringExtra("date") ?: "2024.05.11"
        tvDate.text = today

        // 5) 코루틴에서 DB 조회 → 어댑터에 전달
        lifecycleScope.launch {
            val list: List<DeliveryHistory> =
                db.deliveryHistoryDao().findByDate(today)
            adapter.submitList(list)
        }
    }
}
