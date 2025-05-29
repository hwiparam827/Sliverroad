package com.example.sliverroad

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 3) 배송 내역 이미지 버튼 클릭 처리
        val btnHistory = findViewById<ImageButton>(R.id.btnHistory)
        btnHistory.setOnClickListener {
            // HistoryActivity 로 이동
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // 4) 출근하기 이미지 버튼 클릭 처리
        val btnStart = findViewById<ImageButton>(R.id.btnStartWork)
        btnStart.setOnClickListener {
            // TODO: 출근하기 로직 (예: 서버 호출 후 상태 변경)
            startActivity(Intent(this, CallWaitingActivity::class.java))

        }
    }
}
