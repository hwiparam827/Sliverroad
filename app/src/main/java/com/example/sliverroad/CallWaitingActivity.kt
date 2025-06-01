package com.example.sliverroad

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.sliverroad.api.ApiClient
import com.example.sliverroad.data.CallRequest
import com.example.sliverroad.data.LocationRequest
import com.example.sliverroad.data.LocationResponse
import com.example.sliverroad.data.LoginStatusRequest
import com.example.sliverroad.data.CallStatusResponse
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.RequestBody.Companion.toRequestBody

class CallWaitingActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    // — WebSocket 전역 변수
    private var assignmentSocket: WebSocket? = null  // 콜 할당 전용
    private var locationSocket: WebSocket? = null    // 위치 수신 전용

    // — UI 컴포넌트 바인딩
    private lateinit var tvStatus: TextView
    private lateinit var llMainButtons: LinearLayout

    private lateinit var btnStopCall: ImageButton
    private lateinit var btnResumeCall: ImageButton
    private lateinit var btnEndWork: ImageButton
    private lateinit var btnAcceptCall: ImageButton
    private lateinit var btnRejectCall: ImageButton
    private lateinit var btnTestCall: Button

    private lateinit var cvIncomingCall: CardView
    private lateinit var tvIncomingFare: TextView
    private lateinit var tvIncomingPickup: TextView
    private lateinit var tvIncomingDropoff: TextView
    private lateinit var llIncomingButtons: LinearLayout

    // 토큰 (Intent에서 가져옴)
    private lateinit var accessToken: String

    // 현재 수신된 CallRequest (인커밍 콜용)
    private var currentRequest: CallRequest? = null

    // “현재 배달 중인지” 플래그
    private var isDelivering: Boolean = false

    // LocationServices
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_waiting)

        // 1) 뷰 바인딩
        tvStatus         = findViewById(R.id.tvStatus)
        llMainButtons    = findViewById(R.id.llMainButtons)
        btnStopCall      = findViewById(R.id.btnStopCall)
        btnResumeCall    = findViewById(R.id.btnResumeCall)
        btnEndWork       = findViewById(R.id.btnEndWork)
        btnAcceptCall    = findViewById(R.id.btnAcceptCall)
        btnRejectCall    = findViewById(R.id.btnRejectCall)
        btnTestCall      = findViewById(R.id.btnTestCall)

        cvIncomingCall   = findViewById(R.id.cvIncomingCall)
        tvIncomingFare   = findViewById(R.id.tvIncomingFare)
        tvIncomingPickup = findViewById(R.id.tvIncomingPickup)
        tvIncomingDropoff= findViewById(R.id.tvIncomingDropoff)
        llIncomingButtons= findViewById(R.id.llIncomingButtons)

        // 2) Intent에서 토큰 꺼내기
        accessToken = intent.getStringExtra("access_token") ?: ""

        // 3) 위치 권한 확인 → WebSocket 연결 + REST API 위치 전송 루프
        if (checkLocationPermission()) {
            // ➀ 1회 REST API 위치 전송
            sendCurrentLocationOnce()

            // ➁ 위치 수신 WebSocket 시작
            startLocationWebSocket()

            // ➂ 콜 할당 WebSocket 시작
            connectAssignmentSocket()

            // ➃ 10초마다 REST API로 위치 전송
            lifecycleScope.launch(Dispatchers.IO) {
                while (isActive) {
                    sendCurrentLocationOnce()
                    delay(10_000)
                }
            }
        } else {
            requestLocationPermission()
        }

        // 초기 상태 텍스트
        tvStatus.text = "콜 대기중\n···"
        btnResumeCall.visibility = View.GONE

        // 4) 버튼 리스너 설정

        // 4-1) 배달 거절(인커밍 콜 거절) 버튼
        btnRejectCall.setOnClickListener {
            hideIncomingCall()
            // assignmentSocket은 계속 유지 → 다음 요청 수신 가능
        }

        // 4-2) 배달 수락 버튼
        btnAcceptCall.setOnClickListener {
            currentRequest?.let { req ->
                isDelivering = true

                // 콜 할당 WebSocket 닫기 (배달 중엔 새 요청 무시)
                assignmentSocket?.close(1000, "accepted")
                assignmentSocket = null
                Log.d("WebSocket", "🟠 배달 수락 → assignmentSocket 닫음, isDelivering=true")

                // 배달 상세/진행 화면으로 이동
                val intent = Intent(this@CallWaitingActivity, CallInfoActivity::class.java).apply {
                    putExtra("access_token", accessToken)
                    putExtra("request_id", req.id)
                }
                startActivity(intent)
                hideIncomingCall()
            }
        }

        // 4-3) 콜 멈춤 버튼: REST API로 can_receive_call=false, 화면 상태만 변경
        btnStopCall.setOnClickListener {
            // 화면 상태 변경
            tvStatus.text = "콜 멈춤\n..."
            btnResumeCall.visibility = View.VISIBLE
            btnStopCall.visibility = View.GONE

            // 메인 버튼 표시, 수락/거절 숨김
            llMainButtons.visibility = View.VISIBLE
            btnAcceptCall.visibility = View.GONE
            btnRejectCall.visibility = View.GONE

            // REST API: can_receive_call = false
            val json = JSONObject().apply { put("can_receive_call", false) }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            ApiClient.apiService.toggleCallReceiveStatus("Bearer $accessToken", body)
                .enqueue(object : Callback<CallStatusResponse> {
                    override fun onResponse(
                        call: Call<CallStatusResponse>,
                        response: Response<CallStatusResponse>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("API", "콜 수신 중지 성공")
                        } else {
                            Log.e(
                                "API",
                                "콜 수신 중지 실패: ${response.code()}, ${response.errorBody()?.string()}"
                            )
                        }
                    }
                    override fun onFailure(call: Call<CallStatusResponse>, t: Throwable) {
                        Log.e("API", "콜 수신 중지 오류", t)
                    }
                })

            // 콜 할당 WebSocket 닫기
            assignmentSocket?.close(1000, "stopped")
            assignmentSocket = null
            Log.d("WebSocket", "🟠 콜 멈춤 → assignmentSocket 닫음")
        }

        // 4-4) 콜 재개 버튼: REST API로 can_receive_call=true, 화면 상태만 변경
        btnResumeCall.setOnClickListener {
            // 화면 상태 변경
            tvStatus.text = "콜 대기중\n···"
            btnResumeCall.visibility = View.GONE
            btnStopCall.visibility = View.VISIBLE

            // 메인 버튼 표시, 수락/거절 숨김
            llMainButtons.visibility = View.VISIBLE
            btnAcceptCall.visibility = View.GONE
            btnRejectCall.visibility = View.GONE

            // REST API: can_receive_call = true
            val json = JSONObject().apply { put("can_receive_call", true) }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
            ApiClient.apiService.toggleCallReceiveStatus("Bearer $accessToken", body)
                .enqueue(object : Callback<CallStatusResponse> {
                    override fun onResponse(
                        call: Call<CallStatusResponse>,
                        response: Response<CallStatusResponse>
                    ) {
                        if (response.isSuccessful) {
                            Log.d("API", "콜 수신 재개 성공")
                        } else {
                            Log.e(
                                "API",
                                "콜 수신 재개 실패: ${response.code()}, ${response.errorBody()?.string()}"
                            )
                        }
                    }
                    override fun onFailure(call: Call<CallStatusResponse>, t: Throwable) {
                        Log.e("API", "콜 수신 재개 오류", t)
                    }
                })

            // 콜 할당 WebSocket 재연결 (isDelivering == false일 때만)
            connectAssignmentSocket()
            Log.d("WebSocket", "▶ 콜 재개 → assignmentSocket 재연결")
        }

        // 4-5) 퇴근 버튼: 두 WebSocket 닫고, 서버에 퇴근 상태 전송 → 액티비티 종료
        btnEndWork.setOnClickListener {
            // 콜 할당 WebSocket 닫기
            assignmentSocket?.close(1000, "end_work")
            assignmentSocket = null
            Log.d("WebSocket", "🟠 퇴근 → assignmentSocket 닫음")

            // 위치 WebSocket 닫기
            locationSocket?.close(1000, "end_work_loc")
            locationSocket = null
            Log.d("LocationSocket", "🟠 퇴근 → locationSocket 닫음")

            // REST API: is_working = false
            val statusRequest = LoginStatusRequest(is_working = false)
            ApiClient.apiService.changeWorkingStatus("Bearer $accessToken", statusRequest)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Log.d("API", "퇴근 처리 완료")
                        } else {
                            Log.e("API", "퇴근 처리 실패: ${response.code()}")
                        }
                        finish()
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("API", "퇴근 처리 오류", t)
                        finish()
                    }
                })
        }

        // 4-6) 테스트 콜 (로컬용)
        btnTestCall.setOnClickListener {
            val fake = CallRequest(
                id      = "ada",
                fare    = 23000,
                pickup  = "강남구 역삼동",
                dropoff = "동대문구 회기동"
            )
            currentRequest = fake
            showIncomingCall(fake)
        }
    }

    override fun onResume() {
        super.onResume()
        // CallInfoActivity에서 배달 완료 후 돌아왔을 때
        if (isDelivering) {
            isDelivering = false
            Log.d("WebSocket", "▶ onResume: 배달 완료 후 isDelivering=false → assignmentSocket 재연결")
            connectAssignmentSocket()
        }
    }

    //================================================================================
    // 1) 콜 할당 WebSocket 연결 (/ws/assignments/)
    //================================================================================
    private fun connectAssignmentSocket() {
        // 이미 연결되었거나(isDelivering==true)이면 재연결하지 않음
        if (assignmentSocket != null || isDelivering) return

        Log.d("WebSocket", "▶ connectAssignmentSocket 직전 accessToken = '$accessToken'")
        val request = Request.Builder()
            .url("wss://largeredjade.site/ws/assignments/?token=$accessToken")
            .build()

        assignmentSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                super.onOpen(webSocket, response)
                Log.d("WebSocket", "🟢 콜 할당 WebSocket 연결 성공 (/ws/assignments/)")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                runOnUiThread {
                    Log.d("WebSocket", "📨 서버가 보낸 원본 메시지: $text")

                    try {
                        val data = JSONObject(text)
                        if (data.getString("type") == "assignment") {
                            if (isDelivering) {
                                Log.d("WebSocket", "📨 assignment 도착했지만 이미 배달 중이므로 무시")
                                return@runOnUiThread
                            }
                            val assignment = data.getJSONObject("assignment")
                            Log.d("WebSocket", "assignment 오브젝트: $assignment")

                            // ID: Int → String
                            val id = assignment.getInt("id").toString()

                            // pickup_location / delivery_location
                            val pickupLocation   = assignment.optString("pickup_location", "알 수 없음")
                            val deliveryLocation = assignment.optString("delivery_location", "알 수 없음")

                            // fare 무작위 생성 (1000원~30000원)
                            val fare = (1000..30000).random()

                            val callRequest = CallRequest(
                                id      = id,
                                fare    = fare,
                                pickup  = pickupLocation,
                                dropoff = deliveryLocation
                            )
                            currentRequest = callRequest
                            showIncomingCall(callRequest)
                        }
                    } catch (e: Exception) {
                        Log.e("WebSocket", "onMessage 파싱 에러", e)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                Log.d("WebSocket", "🟠 onClosing (assignment WS): 코드=$code, 이유=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Log.d("WebSocket", "🔴 onClosed (assignment WS): 코드=$code, 이유=$reason")
                assignmentSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                super.onFailure(webSocket, t, response)
                Log.e("WebSocket", "❌ onFailure (assignment WS): 연결 실패", t)
                assignmentSocket = null
            }
        })
    }

    //================================================================================
    // 2) 위치 수신 WebSocket 연결 (/ws/courier/location/)
    //================================================================================
    @SuppressLint("MissingPermission")
    private fun startLocationWebSocket() {
        val wsUrl = "wss://largeredjade.site/ws/courier/location/?token=$accessToken"
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        locationSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                super.onOpen(webSocket, response)
                Log.d("LocationSocket", "🟢 위치 수신 WebSocket 연결 성공")

                // (원한다면 서버에서 오는 위치 데이터를 처리)
                runOnUiThread {
                    // 예: 위치 데이터를 표시에 활용할 수 있음
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                runOnUiThread {
                    // 서버가 보내는 위치 JSON을 받고 싶다면 여기서 파싱
                    Log.d("LocationSocket", "📨 위치 데이터 수신: $text")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                super.onFailure(webSocket, t, response)
                Log.e("LocationSocket", "❌ 위치 WS 연결 실패", t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                Log.d("LocationSocket", "🟠 onClosing (location WS): 코드=$code, 이유=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                Log.d("LocationSocket", "🔴 onClosed (location WS): 코드=$code, 이유=$reason")
                locationSocket = null
            }
        })
    }

    //================================================================================
    // 3) REST API로 1회 위치 전송 (PATCH /api/delivery/location/)
    //================================================================================
    @SuppressLint("MissingPermission")
    private fun sendCurrentLocationOnce() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Permission", "위치 권한 없음. 위치 전송 중단.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                loc?.let {
                    val locationRequest = LocationRequest(
                        latitude = it.latitude,
                        longitude = it.longitude
                    )

                    ApiClient.apiService.updateLocation(
                        "Bearer $accessToken",
                        locationRequest
                    ).enqueue(object : Callback<LocationResponse> {
                        override fun onResponse(
                            call: Call<LocationResponse>,
                            response: Response<LocationResponse>
                        ) {
                            if (response.isSuccessful) {
                                Log.d("API", "위치 최신화 성공: ${response.body()}")
                            } else {
                                Log.e(
                                    "API",
                                    "위치 최신화 실패: ${response.code()}, ${response.errorBody()?.string()}"
                                )
                            }
                        }

                        override fun onFailure(call: Call<LocationResponse>, t: Throwable) {
                            Log.e("API", "위치 최신화 오류", t)
                        }
                    })
                }
            }
    }

    //================================================================================
    // 4) 인커밍 콜 UI 표시 (수락/거절 버튼 노출)
    //================================================================================
    private fun showIncomingCall(request: CallRequest) {
        btnAcceptCall.visibility = View.VISIBLE
        btnRejectCall.visibility = View.VISIBLE

        tvStatus.visibility      = View.GONE
        llMainButtons.visibility = View.GONE

        tvIncomingFare.text      = "배송료 ${request.fare}원"
        tvIncomingPickup.text    = request.pickup
        tvIncomingDropoff.text   = request.dropoff

        cvIncomingCall.visibility    = View.VISIBLE
        llIncomingButtons.visibility = View.VISIBLE
    }

    //================================================================================
    // 5) 인커밍 콜 UI 숨기기
    //================================================================================
    private fun hideIncomingCall() {
        cvIncomingCall.visibility    = View.GONE
        llIncomingButtons.visibility = View.GONE

        tvStatus.visibility      = View.GONE
        llMainButtons.visibility = View.VISIBLE

        btnAcceptCall.visibility = View.GONE
        btnRejectCall.visibility = View.GONE

        currentRequest = null
    }

    //================================================================================
    // 6) 권한 체크/요청
    //================================================================================
    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 권한 허용 후 다시 위치 1회 전송 + WebSocket 연결 + REST 반복 전송
                sendCurrentLocationOnce()
                startLocationWebSocket()
                connectAssignmentSocket()
                lifecycleScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        sendCurrentLocationOnce()
                        delay(10_000)
                    }
                }
            } else {
                Log.e("Permission", "위치 권한이 거부되었습니다.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 액티비티가 종료될 때 두 WebSocket 모두 닫기
        assignmentSocket?.close(1000, "activity_destroyed")
        assignmentSocket = null
        locationSocket?.close(1000, "activity_destroyed")
        locationSocket = null
    }
}

