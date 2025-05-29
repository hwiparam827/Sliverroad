package com.example.sliverroad

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val imageView = findViewById<ImageView>(R.id.resultImageView)
        val photoUri = intent.getStringExtra("photoUri")

        if (photoUri != null) {
            imageView.setImageURI(Uri.parse(photoUri))
        }
    }
}
