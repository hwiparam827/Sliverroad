package com.example.sliverroad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.sliverroad.api.ApiClient.apiService
import com.example.sliverroad.api.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class DeliveryFinishActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnPhotoAdd: ImageButton
    private var savedPhotoUri: Uri? = null
    private lateinit var accessToken: String
    private var assignmentId: Int = -1

    companion object {
        const val REQUEST_PHOTO_CAPTURE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_finish)

        imagePreview = findViewById(R.id.imagePreview)
        btnPhotoAdd = findViewById(R.id.btnPhotoAdd)

        accessToken = intent.getStringExtra("access_token") ?: ""
        assignmentId = intent.getIntExtra("assignment_id", -1)

        btnPhotoAdd.setOnClickListener {
            val intent = Intent(this, PhotoCaptureActivity::class.java)
            startActivityForResult(intent, REQUEST_PHOTO_CAPTURE)
        }

        findViewById<ImageButton>(R.id.btn_finish).setOnClickListener {
            if (savedPhotoUri != null) {
                uploadPhotoAndCompleteDelivery(savedPhotoUri!!)
                val intent = Intent(this, CallWaitingActivity::class.java)
                // 스택 최상위에 MainActivity만 남기고 다 지우기
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish() // 현재 액티비티 종료
            } else {
                Toast.makeText(this, "사진을 먼저 촬영해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PHOTO_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val uriString = data.getStringExtra("photoUri")
            uriString?.let {
                val photoUri = Uri.parse(it)
                savedPhotoUri = photoUri
                imagePreview.setImageURI(photoUri)
                btnPhotoAdd.visibility = View.GONE
            }
        }
    }

    private fun uploadPhotoAndCompleteDelivery(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            Toast.makeText(this, "파일 열기 실패", Toast.LENGTH_SHORT).show()
            return
        }

        val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
        tempFile.outputStream().use { fileOut -> inputStream.copyTo(fileOut) }

        val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("photo", tempFile.name, requestFile)

        val id = assignmentId
        if (id == null || id == -1) {
            Toast.makeText(this, "📛 잘못된 assignment_id", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.completeDeliveryWithPhoto("Bearer $accessToken", id, body)
            .enqueue(object : Callback<ApiService.CompleteDeliveryResponse> {
                override fun onResponse(
                    call: Call<ApiService.CompleteDeliveryResponse>,
                    response: Response<ApiService.CompleteDeliveryResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DeliveryFinishActivity, "✅ 배달 완료!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@DeliveryFinishActivity, CallWaitingActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                        finish()
                    } else {
                        Toast.makeText(this@DeliveryFinishActivity, "❌ 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiService.CompleteDeliveryResponse>, t: Throwable) {
                    Toast.makeText(this@DeliveryFinishActivity, "❌ 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

}
