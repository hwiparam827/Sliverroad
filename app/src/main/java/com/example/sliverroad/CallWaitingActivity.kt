package com.example.sliverroad

import android.content.Intent            // ← Intent, putExtra 쓰려면 추가
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.sliverroad.data.AppDatabase
import com.example.sliverroad.data.CallRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.util.Log

class CallWaitingActivity : AppCompatActivity() {

    // 1) 상태 텍스트 + 메인 버튼 그룹
    private lateinit var tvStatus: TextView
    private lateinit var llMainButtons: LinearLayout

    // 2) 인커밍 콜 UI
    private lateinit var cvIncomingCall: CardView
    private lateinit var tvIncomingFare: TextView
    private lateinit var tvIncomingPickup: TextView
    private lateinit var tvIncomingDropoff: TextView
    private lateinit var llIncomingButtons: LinearLayout
    private lateinit var btnRejectCall: ImageButton
    private lateinit var btnAcceptCall: ImageButton

    // 3) 기본 버튼들 + 테스트 콜
    private lateinit var btnTestCall: Button
    private lateinit var btnStopCall: ImageButton
    private lateinit var btnResumeCall: ImageButton
    private lateinit var btnEndWork: ImageButton

    private var sendingEnabled = true
    private var currentRequest: CallRequest? = null

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private val db by lazy { AppDatabase.getInstance(this) }
    private val callDao by lazy { db.callRequestDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_waiting)

        // bind views
        tvStatus        = findViewById(R.id.tvStatus)
        llMainButtons   = findViewById(R.id.llMainButtons)

        btnTestCall     = findViewById(R.id.btnTestCall)
        btnStopCall     = findViewById(R.id.btnStopCall)
        btnResumeCall   = findViewById(R.id.btnResumeCall)
        btnEndWork      = findViewById(R.id.btnEndWork)

        cvIncomingCall     = findViewById(R.id.cvIncomingCall)
        tvIncomingFare     = findViewById(R.id.tvIncomingFare)
        tvIncomingPickup   = findViewById(R.id.tvIncomingPickup)
        tvIncomingDropoff  = findViewById(R.id.tvIncomingDropoff)
        llIncomingButtons  = findViewById(R.id.llIncomingButtons)
        btnRejectCall      = findViewById(R.id.btnRejectCall)
        btnAcceptCall      = findViewById(R.id.btnAcceptCall)


        // 콜 멈춤 / 대기중
        btnStopCall.setOnClickListener {
            sendingEnabled = false
            tvStatus.text = "콜 멈춤"
        }
        btnResumeCall.setOnClickListener {
            sendingEnabled = true
            tvStatus.text = "콜 대기중\n···"
        }

        // 퇴근
        btnEndWork.setOnClickListener { finish() }


        // 테스트 콜
        btnTestCall.setOnClickListener {
            val fake = CallRequest(
                id      = 999,
                fare    = 23000,
                pickup  = "강남구 역삼동",
                dropoff = "동대문구 회기동",
                lat     = 37.4979,
                lng     = 127.0276,
                handled = false
            )
            currentRequest = fake   // ✅ 이 줄이 필요합니다!
            showIncomingCall(fake)
        }
        // 거절
        btnRejectCall.setOnClickListener {
            hideIncomingCall()
        }
        btnAcceptCall.setOnClickListener {
            currentRequest?.let { req ->
                lifecycleScope.launch {
                    callDao.update(req.copy(handled = true))
                }

                val intent = Intent(this, CallInfoActivity::class.java).apply {
                    putExtra("fare", req.fare)
                    putExtra("pickup", req.pickup)
                    putExtra("dropoff", req.dropoff)
                    putExtra("id", req.id)
                }

                Log.d("CallAccept", "콜 수락됨 → 화면 전환 시도")
                startActivity(intent)  // ← 이거 실행되는지 확인
            }
        }

        // 위치 전송 + 새 콜 감시
        lifecycleScope.launch {
            sendCurrentLocation()
            callDao.findNearestUnHandled(0.0, 0.0)
                .collectLatest { req ->
                    if (req != null) {
                        currentRequest = req
                        showIncomingCall(req)
                    } else {
                        hideIncomingCall()
                    }
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendCurrentLocation() {
        if (!sendingEnabled) return
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                loc?.let {
                    // TODO: 서버/DB 전송
                }
            }
    }

    // — 모르는 참조 삭제, llMainButtons 바인딩 후 show/hide 구현
    private fun showIncomingCall(request: CallRequest) {
        btnAcceptCall.visibility = View.VISIBLE
        btnRejectCall.visibility = View.VISIBLE

        tvStatus.visibility      = View.GONE
        llMainButtons.visibility = View.GONE

        tvIncomingFare.text      = "배송료 ${request.fare}원"
        tvIncomingPickup.text    = request.pickup
        tvIncomingDropoff.text   = request.dropoff

        cvIncomingCall.visibility     = View.VISIBLE
        llIncomingButtons.visibility  = View.VISIBLE
    }

    private fun hideIncomingCall() {
        cvIncomingCall.visibility     = View.GONE
        llIncomingButtons.visibility  = View.GONE

        tvStatus.visibility      = View.VISIBLE
        llMainButtons.visibility = View.VISIBLE
    }
}
