package com.example.sliverroad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.view.View

class DeliveryFinishActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnPhotoAdd: ImageButton

    companion object {
        const val REQUEST_PHOTO_CAPTURE = 100 // PhotoCaptureActivity와 동일한 requestCode 사용
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_finish)  // XML 레이아웃 설정

        imagePreview = findViewById(R.id.imagePreview)
        btnPhotoAdd = findViewById(R.id.btnPhotoAdd)

        btnPhotoAdd.setOnClickListener {
            val intent = Intent(this, PhotoCaptureActivity::class.java)
            startActivityForResult(intent, REQUEST_PHOTO_CAPTURE)
        }
        val btn_finish2 = findViewById<ImageButton>(R.id.btn_finish)

        btn_finish2.setOnClickListener {
            val intent = Intent(this, CallWaitingActivity::class.java)
            // 스택 최상위에 MainActivity만 남기고 다 지우기
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() // 현재 액티비티 종료
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PHOTO_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val uriString = data.getStringExtra("photoUri")
            uriString?.let {
                val photoUri = Uri.parse(it)
                imagePreview.setImageURI(photoUri)
                btnPhotoAdd.visibility = View.GONE
            }
        }
    }

}
