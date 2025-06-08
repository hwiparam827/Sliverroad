package com.example.sliverroad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sliverroad.api.ApiClient
import com.example.sliverroad.api.ApiService
import com.example.sliverroad.data.CallRequestDetailResponse
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class FindBenchActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnPhotoAdd: ImageButton
    private var savedPhotoUri: Uri? = null
    private lateinit var accessToken: String
    private var assignmentId: Int = -1
    private lateinit var deliverytime: TextView
    private lateinit var deliveryweight: TextView
    private lateinit var requestId: String

    companion object {
        const val REQUEST_PHOTO_CAPTURE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_finish)

        requestId = intent.getStringExtra("request_id") ?: ""
        accessToken = intent.getStringExtra("access_token") ?: ""
        assignmentId = intent.getIntExtra("assignment_id", -1)

        imagePreview = findViewById(R.id.imagePreview)
        btnPhotoAdd = findViewById(R.id.btnPhotoAdd)
        deliverytime = findViewById(R.id.deliverytime)
        deliveryweight = findViewById(R.id.deliveryweight)

        btnPhotoAdd.setOnClickListener {
            val intent = Intent(this, PhotoCaptureActivity::class.java)
            startActivityForResult(intent, REQUEST_PHOTO_CAPTURE)
        }

        findViewById<ImageButton>(R.id.btn_finish).setOnClickListener {
            if (savedPhotoUri != null) {
                uploadPhotoAndCompleteDelivery(savedPhotoUri!!)
                val intent = Intent(this, CallWaitingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("access_token", accessToken)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "사진을 먼저 촬영해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        fetchRequestDetail()
    }

    private fun fetchRequestDetail() {
        val bearer = "Bearer $accessToken"
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getDeliveryDetail(bearer, requestId)
                if (response.isSuccessful && response.body() != null) {
                    Log.d("CallInfo", "상세 조회 성공, bindDetail 호출")
                    bindDetail(response.body()!!)
                } else {
                    Log.e("CallInfo", "상세 조회 실패: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CallInfo", "상세 조회 오류", e)
            }
        }
    }

    private fun bindDetail(detail: CallRequestDetailResponse) {
        val isoTime = detail.pickup_time
        val timeText = if (!isoTime.isNullOrBlank()) {
            formatIsoTimeLegacy(isoTime)
        } else {
            getCurrentFormattedTime()
        }
        deliverytime.text = timeText
        deliveryweight.text = "${detail.item_weight ?: "-"}g"
    }

    private fun formatIsoTimeLegacy(iso: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(iso)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            outputFormat.format(date!!)
        } catch (e: Exception) {
            "시간 형식 오류"
        }
    }

    private fun getCurrentFormattedTime(): String {
        val now = System.currentTimeMillis()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(now)
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
        val photoBody = MultipartBody.Part.createFormData("photo", tempFile.name, requestFile)

        val id = assignmentId
        if (id == null || id == -1) {
            Toast.makeText(this, "📛 잘못된 assignment_id", Toast.LENGTH_SHORT).show()
            return
        }

        ApiClient.apiService.completeDeliveryWithPhoto("Bearer $accessToken", assignmentId, photoBody)
            .enqueue(object : Callback<ApiService.CompleteDeliveryResponse> {
                override fun onResponse(
                    call: Call<ApiService.CompleteDeliveryResponse>,
                    response: Response<ApiService.CompleteDeliveryResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@FindBenchActivity, "✅ 업데이트 완료!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@FindBenchActivity, "❌ 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiService.CompleteDeliveryResponse>, t: Throwable) {
                    Toast.makeText(this@FindBenchActivity, "❌ 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

}

