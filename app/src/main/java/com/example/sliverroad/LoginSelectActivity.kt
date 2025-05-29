package com.example.sliverroad

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import com.example.sliverroad.LoginActivity

// …

class LoginSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_select)

        // ImageView 로 받아오기
        val btnLogin = findViewById<ImageView>(R.id.btnLogin13)
        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
