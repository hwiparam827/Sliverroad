package com.example.sliverroad

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sliverroad.api.ApiClient
import com.example.sliverroad.api.ApiClient.apiService
import com.example.sliverroad.data.CallRequestDetailResponse
import com.example.sliverroad.databinding.ActivityCallWaitingBinding
import com.example.sliverroad.model.FindRouteResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.sliverroad.viewmodel.DriverViewModel
import androidx.activity.viewModels   // Activity에서


class CallInfoActivity : AppCompatActivity() {

    // Intent로 전달받을 토큰과 requestId, CallRequest
    private lateinit var accessToken: String
    private lateinit var requestId: String
    private var assignmentId: Int? = null

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
    private lateinit var binding: ActivityCallWaitingBinding
    private val viewModel: DriverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_info)


        accessToken = intent.getStringExtra("access_token") ?: ""

        // ───────────────────────────────────────────────────────────────────
        // 1) Intent에서 CallRequest 객체와 accessToken, requestId 꺼내기
        // ───────────────────────────────────────────────────────────────────
        accessToken = intent.getStringExtra("access_token") ?: ""
        requestId = intent.getStringExtra("request_id") ?: ""
        assignmentId = intent.getIntExtra("assignment_id", -1)

        // ───────────────────────────────────────────────────────────────────
        // 2) 필수 값 존재 여부 체크
        // ───────────────────────────────────────────────────────────────────
        if (accessToken.isBlank() || requestId.isBlank()) {
            Log.e("CallInfo", "토큰 또는 requestId가 없습니다.")
            finish()
            return
        }

        // ───────────────────────────────────────────────────────────────────
        // 3) 뷰 바인딩
        // ───────────────────────────────────────────────────────────────────
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

        // ───────────────────────────────────────────────────────────────────
        // 4) 서버에서 상세 정보 가져오기
        // ───────────────────────────────────────────────────────────────────
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

        // ───────────────────────────────────────────────────────────────────
        // 5) “경로 안내” 버튼 클릭 시: 먼저 “pickup” 경로 요청 → 성공 시 OsmMapActivity로 이동
        // ───────────────────────────────────────────────────────────────────
        btnStartNavigation.setOnClickListener {
            val body = mapOf("destination_type" to "pickup")
            apiService.findRoute(
                authHeader = "Bearer $accessToken",
                requestId = requestId,
                body = body
            ).enqueue(object: Callback<FindRouteResponse> {
                override fun onResponse(
                    call: Call<FindRouteResponse>,
                    response: Response<FindRouteResponse>
                ) {
                    if (response.isSuccessful) {
                        // 네트워크 응답으로 받아온 객체
                        val data = response.body()!!

                        // 1) 예시: 받을 수 있는 key들을 로그에 찍어보기
                        Log.d("Route", "request_id = ${data.request_id}")
                        Log.d("Route", "requester_phone = ${data.requester_phone}")
                        Log.d("Route", "origin = ${data.origin.latitude}, ${data.origin.longitude}")
                        Log.d("Route", "destination = ${data.destination.latitude}, ${data.destination.longitude}")
                        Log.d("Route", "destination_type = ${data.destination_type}")

                        // 2) 네 가지 경로 중에서 “shortest” 경로 정보 꺼내기
                        val pickupInfo = data.routes["shortest"]
                           if (pickupInfo != null) {

                                   // 보행 좌표, 벤치 좌표 등은 pickupInfo를 통해 읽어야 합니다.
                                   val walkCoords: List<List<Double>> = pickupInfo.walk_route.coordinates
                                   if (walkCoords.isNotEmpty()) {
                                           val firstLat = walkCoords[0][0]
                                           val firstLng = walkCoords[0][1]
                                           Log.d("Route", "첫 좌표 (pickup): lat=$firstLat, lng=$firstLng")
                                        }
                               val benches: List<List<Double>> = pickupInfo.benches
                               Log.d("Route", "벤치 개수 (pickup) = ${benches.size}")
                               }

                        // 4) (선택) OsmMapActivity로 보낼 때 JSON으로 통째로 넘기고 싶다면 Gson 사용
                        val gson = Gson()
                        val jsonAllRoutes = gson.toJson(data.routes)    // Map<String, RouteInfo> 전체를 JSON string으로
                        val originJson      = gson.toJson(data.origin)
                        val destinationJson = gson.toJson(data.destination)


                        val intent = Intent(this@CallInfoActivity, OsmMapActivity::class.java).apply {
                            putExtra("access_token", accessToken)
                            putExtra("request_id", requestId)
                            putExtra("assignment_id", assignmentId)

                            // 길찾기 결과를 JSON String으로 넘기는 예시
                            putExtra("routes_json", jsonAllRoutes)
                            putExtra("origin_json", originJson)
                            putExtra("destination_json", destinationJson)
                        }
                        startActivity(intent)

                    } else {
                        Log.e("API", "findRoute 실패: HTTP ${response.code()} / ${response.errorBody()?.string()}")
                        Toast.makeText(this@CallInfoActivity, "경로 조회에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FindRouteResponse>, t: Throwable) {
                    Log.e("API", "findRoute 네트워크 오류", t)
                    Toast.makeText(this@CallInfoActivity, "경로 조회 네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
        }


}}



