package com.example.sliverroad

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sliverroad.api.ApiClient
import com.example.sliverroad.data.CallRequestDetailResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.Uri

class CallInfoActivity : AppCompatActivity() {

    // Intent로 전달받을 토큰과 requestId
    private lateinit var accessToken: String
    private lateinit var requestId: String

    // 뷰 바인딩용 멤버 변수
    private lateinit var tvFare: TextView
    private lateinit var tvItemType: TextView
    private lateinit var tvItemName: TextView
    private lateinit var tvItemValue: TextView
    private lateinit var tvItemWeight: TextView
    private lateinit var tvClientName: TextView
    private lateinit var tvClientPhone: TextView
    private lateinit var tvPickupdate: TextView
    private lateinit var tvPickupAddr: TextView
    private lateinit var tvReceiverName: TextView
    private lateinit var tvReceiverPhone: TextView
    private lateinit var tvDropoffAddr: TextView
    private lateinit var tvNote: TextView
    private lateinit var btnStartNavigation: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_info)

        // 1) 인텐트로부터 토큰과 requestId 가져오기
        accessToken = intent.getStringExtra("access_token") ?: ""
        if (accessToken.isBlank() || requestId.isBlank()) {
            Log.e("CallInfo", "토큰 또는 requestId가 없습니다.")
            finish()
            return
        }

        // 2) 뷰 바인딩
        tvFare = findViewById(R.id.tvInfoFare)
        tvItemType = findViewById(R.id.tvItemType)
        tvItemName = findViewById(R.id.tvItemName)
        tvItemValue = findViewById(R.id.tvItemValue)
        tvItemWeight = findViewById(R.id.tvItemWeight)
        tvClientName = findViewById(R.id.tvClientName)
        tvClientPhone = findViewById(R.id.tvClientPhone)
        tvPickupdate = findViewById(R.id.tvPickupdate)
        tvPickupAddr = findViewById(R.id.tvPickupAddr)
        tvReceiverName = findViewById(R.id.tvReceiverName)
        tvReceiverPhone = findViewById(R.id.tvReceiverPhone)
        tvDropoffAddr = findViewById(R.id.tvDropoffAddr)
        tvNote = findViewById(R.id.tvNote)
        btnStartNavigation = findViewById(R.id.btnStartNavigation)

        // 3) 서버에서 상세 정보 가져오기
        fetchRequestDetail()
    }

    private fun fetchRequestDetail() {
        val bearer = "Bearer $accessToken"
        ApiClient.apiService
            .getRequestDetail(bearer, requestId)
            .enqueue(object : Callback<CallRequestDetailResponse> {
                override fun onResponse(
                    call: Call<CallRequestDetailResponse>,
                    response: Response<CallRequestDetailResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        bindDetail(response.body()!!)
                    } else {
                        Log.e("Detail", "상세 조회 실패: HTTP ${response.code()}")
                        finish()
                    }
                }

                override fun onFailure(call: Call<CallRequestDetailResponse>, t: Throwable) {
                    Log.e("Detail", "상세 조회 오류", t)
                    finish()
                }
            })
    }

    private fun bindDetail(detail: CallRequestDetailResponse) {
        // 서버에서 받은 데이터를 화면에 뿌리기
        tvItemType.text = "물품: ${detail.item_type}"
        tvItemName.text = "물품명: ${detail.item_name}"
        tvItemValue.text = "물품 액면가: ${detail.item_value}"
        tvItemWeight.text = "물품 무게: ${detail.item_weight}"
        tvClientName.text = "의뢰인: ${detail.requester_name}"
        tvClientPhone.text = "의뢰인 연락처: ${detail.requester_phone}"
        tvPickupdate.text = "픽업 시간: ${detail.pickup_time}"
        tvPickupAddr.text = "픽업 장소: ${detail.pickup_location}"
        tvReceiverName.text = "수취인: ${detail.recipient_name}"
        tvReceiverPhone.text = "수취인 연락처: ${detail.recipient_phone}"
        tvDropoffAddr.text = "배달 장소: ${detail.address}"
        tvNote.text = "지침: ${detail.instructions}"

        // 만약 배송요금(fare) 필드가 응답 JSON에 없다면 주석 처리하거나 제거하세요.
        // tvFare.text = "배송료: ${detail.fare}"

        // — 사진 배열(detail.photos)도 필요하다면, 추후에 추가할 수 있습니다.

        // 네비게이션 버튼 클릭 시 지도 앱 또는 자체 맵 화면으로 이동
        btnStartNavigation.setOnClickListener {
            // 예시: Google Maps 내비게이션 호출
            // "geo:lat,lng?q=<pickupLocation>"
            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(detail.pickup_location)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            }
        }
    }
}
