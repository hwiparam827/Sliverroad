// OsmMapActivity.kt
package com.example.sliverroad

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sliverroad.api.ApiClient.apiService
import com.example.sliverroad.api.ApiService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.example.sliverroad.model.IndividualRoute   // ← 반드시 추가
import com.example.sliverroad.model.FindRouteResponse // ← 필요에 따라 추가
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.example.sliverroad.model.LatLng

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class OsmMapActivity : AppCompatActivity() {

    private lateinit var accessToken: String
    private lateinit var requestId: String
    private lateinit var mapView: MapView
    private lateinit var clientPhoneNumber: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var myLocationMarker: Marker
    private var locationRequest: LocationRequest? = null
    private var assignmentId: Int? = null

    // 서버에서 내려온 routes 맵 전체를 저장할 변수
    private var allRoutes: Map<String, IndividualRoute> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accessToken = intent.getStringExtra("access_token") ?: ""
        requestId = intent.getStringExtra("request_id") ?: ""
        assignmentId = intent.getIntExtra("assignment_id", -1)

        // 1) osm 설정 및 레이아웃 설정
        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_maps)

        // 2) MapView 초기화
        mapView = findViewById(R.id.osm_map)
        mapView.setMultiTouchControls(true)

        // 3) FusedLocationProviderClient 초기화 (내 위치 표시용)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 4) 내 위치 마커 추가 (초기에는 좌표 (0,0)으로 설정됨)
        myLocationMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_location)
        }
        mapView.overlays.add(myLocationMarker)

        // 5) 각 버튼 바인딩
        val btnStartNavigation = findViewById<ImageButton>(R.id.btnStartNavigation)
        val navigationOverlay = findViewById<LinearLayout>(R.id.navigationOverlay)
        val pathSelectionBar = findViewById<LinearLayout>(R.id.pathSelectionBar)
        val btnCallClient = findViewById<ImageButton>(R.id.btnCallClient)
        val btnComplete = findViewById<ImageButton>(R.id.btnComplete)
        val headerCard = findViewById<LinearLayout>(R.id.headerCard)
        val headerCard2 = findViewById<LinearLayout>(R.id.headerCard2)
        val btnSafe = findViewById<ImageButton>(R.id.btnSafe)
        val btnShortest = findViewById<ImageButton>(R.id.btnShortest)
        val btnBench = findViewById<ImageButton>(R.id.btnBench)

        // 6) 위치 권한 검사 후, 업데이트 시작
        checkLocationPermissionAndStartUpdates()

        // 7) Intent 로부터 의뢰인 전화번호 가져오기 (CallWaitingActivity 등에서 putExtra 해 두었다 가정)
        clientPhoneNumber = intent.getStringExtra("clientPhone") ?: ""

        // 8) “경로 안내” 버튼 클릭 시 UI 전환
        btnStartNavigation.setOnClickListener {
            btnStartNavigation.visibility = android.view.View.GONE
            pathSelectionBar.visibility = android.view.View.GONE
            headerCard.visibility = android.view.View.GONE
            navigationOverlay.visibility = android.view.View.VISIBLE
            headerCard2.visibility = android.view.View.VISIBLE
            btnComplete.visibility = android.view.View.VISIBLE

        }

        // 9) “의뢰인에게 전화 걸기” 버튼
        btnCallClient.setOnClickListener {
            makeDirectCall(clientPhoneNumber)
        }

        // 10) “배달 완료” 버튼: 다른 화면(예: DeliveryMapActivity)으로 이동
        // ───────────────────────────────────────────────────────────────────
        btnComplete.setOnClickListener {

            sendDrawableImageToServer {  // 위 함수 호출


            // 3-1) 서버에 “배달(딜리버리)” 경로 요청
            val body = mapOf("destination_type" to "delivery")
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
                        val deliveryinfo = data.routes["shortest"]  // 또는 "safe_path", "bench" 중 선택
                        if (deliveryinfo != null) {
                            // 보행 좌표, 벤치 좌표 등은 pickupInfo를 통해 읽어야 합니다.
                            val walkCoords: List<List<Double>> = deliveryinfo.walk_route.coordinates
                            if (walkCoords.isNotEmpty()) {
                                val firstLat = walkCoords[0][0]
                                val firstLng = walkCoords[0][1]
                                Log.d("Route", "첫 좌표 (pickup): lat=$firstLat, lng=$firstLng")
                            }
                            val benches: List<List<Double>> = deliveryinfo.benches
                            Log.d("Route", "벤치 개수 (pickup) = ${benches.size}")
                        }

                        // 4) (선택) OsmMapActivity로 보낼 때 JSON으로 통째로 넘기고 싶다면 Gson 사용
                        val gson = Gson()
                        val jsonAllRoutes = gson.toJson(data.routes)    // Map<String, RouteInfo> 전체를 JSON string으로
                        val originJson      = gson.toJson(data.origin)
                        val destinationJson = gson.toJson(data.destination)


                        val intent = Intent(this@OsmMapActivity, DeliveryMapActivity::class.java).apply {
                            putExtra("access_token", accessToken)
                            putExtra("request_id", requestId)
                            putExtra("assignment_id", assignmentId)

                            // 길찾기 결과를 JSON String으로 넘기는 예시
                            putExtra("routes_json", jsonAllRoutes)
                            putExtra("origin_json", originJson)
                            putExtra("destination_json", destinationJson)
                        }
                        startActivity(intent)
                        btnComplete.visibility = android.view.View.GONE
                        finish()

                    } else {
                        Log.e("API", "findRoute 실패: HTTP ${response.code()} / ${response.errorBody()?.string()}")
                        Toast.makeText(this@OsmMapActivity, "경로 조회에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<FindRouteResponse>, t: Throwable) {
                    Log.e("API", "findRoute 네트워크 오류", t)
                    Toast.makeText(this@OsmMapActivity, "경로 조회 네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
        }}




        // 11) 서버에서 내려온 routes_json(전체 맵)을 파싱해서 allRoutes에 저장
        //    CallInfoActivity에서 Intent.putExtra("routes_json", gson.toJson(data.routes)) 했다 가정
        val routesJson = intent.getStringExtra("routes_json") ?: "{}"
        allRoutes = Gson().fromJson(
            routesJson,
            object : TypeToken<Map<String, IndividualRoute>>() {}.type
        )

        drawRouteFromData("shortest")  // ✅ 수령지 → 목적지

        btnShortest.setOnClickListener {
            drawRouteFromData("shortest")  // ✅ 수령지 → 목적지
        }

        btnSafe.setOnClickListener {
            drawRouteFromData("safe_path")
        }

        btnBench.setOnClickListener {
            drawRouteFromData("bench")
        }

}
    private fun getColorByWalkType(type: String): Int {
        return when (type) {
            "shortest" -> 0xFF1E90FF.toInt() // 파란색
            "safe_path" -> 0xFFFF0000.toInt() // 빨간색
            "bench" -> 0xFF00FF00.toInt() // 초록색
            else -> 0xFF888888.toInt() // 회색
        }
    }
        private fun checkLocationPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
        } else {
            startLocationUpdates()
        }
    }
    private fun sendDrawableImageToServer(onSuccess: () -> Unit) {
        val inputStream = resources.openRawResource(R.raw.siliver_road)
        val tempFile = File.createTempFile("logo_", ".png", cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("photo", tempFile.name, requestFile)

        apiService.completeRecieptWithPhoto(
            token = "Bearer $accessToken",
            assignmentId = assignmentId!!,
            photo = photoPart
        ).enqueue(object : Callback<ApiService.CompleteRecieptResponse> {
            override fun onResponse(
                call: Call<ApiService.CompleteRecieptResponse>,
                response: Response<ApiService.CompleteRecieptResponse>
            ) {
                if (response.isSuccessful) {
                    Log.d("API", "✅ 수령 완료 처리됨")
                    onSuccess()  // 수령 완료 후 배달 요청 실행
                } else {
                    Log.e("API", "❌ 수령 실패: ${response.code()} / ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ApiService.CompleteRecieptResponse>, t: Throwable) {
                Log.e("API", "❌ 수령 네트워크 오류", t)
            }
        })


}
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val geoPoint = GeoPoint(location.latitude, location.longitude)

                // 내 위치 마커 업데이트
                myLocationMarker.position = geoPoint
                mapView.invalidate()
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest!!,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (securityException: SecurityException) {
            securityException.printStackTrace()
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 서버에서 받아온 allRoutes 맵에서 key에 대응하는 IndividualRoute를 꺼내서 지도에 그립니다.
     * key: "shortest" or "safe_path" or "bench"
     */
    private fun drawRouteFromData(key: String) {
        mapView.overlays.removeAll { it is Polyline || it is Marker }
        mapView.overlays.add(myLocationMarker)

        val route = allRoutes[key] ?: run {
            Toast.makeText(this, "경로('$key') 정보 없음", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ 도보 구간 좌표: origin → 목적지
        val walkPoints = route.walk_route.coordinates.map {
            GeoPoint(it[0], it[1])
        }

        // ✅ 교통 구간 좌표: origin → 목적지
        val transitPoints = route.transit_route.flatMap { segment ->
            segment.coordinates.map { GeoPoint(it[0], it[1]) }
        }

        // 🚶 도보 구간 그리기
        if (walkPoints.size >= 2) {
            val walkLine = Polyline().apply {
                color = getColorByWalkType(key) // walk 경로는 경로마다 색상 다르게
                width = 7.0f
                setPoints(walkPoints)
            }
            mapView.overlays.add(walkLine)
        }

        // 🚍 교통 구간 그리기 (색상 통일)
        if (transitPoints.size >= 2) {
            val transitLine = Polyline().apply {
                color = 0xFF444444.toInt() // 회색
                width = 6.0f
                setPoints(transitPoints)
            }
            mapView.overlays.add(transitLine)
        }

        val originLatLng: LatLng? = intent.getStringExtra("origin_json")?.let {
            Gson().fromJson(it, LatLng::class.java)
        }

        val originPoint = if (originLatLng != null) {
            GeoPoint(originLatLng.latitude, originLatLng.longitude)
        } else if (walkPoints.isNotEmpty()) {
            GeoPoint(walkPoints.first().latitude, walkPoints.first().longitude)
        } else {
            GeoPoint(0.0, 0.0) // fallback
        }
        val startMarker = Marker(mapView).apply {
            position = originPoint
            icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_start)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "출발지"
        }
        mapView.overlays.add(startMarker)

        // 📍 도착지 마커
        val dest = intent.getStringExtra("destination_json")?.let {
            Gson().fromJson(it, LatLng::class.java)
        }
        if (dest != null) {
            val destinationPoint = GeoPoint(dest.latitude, dest.longitude)
            val endMarker = Marker(mapView).apply {
                position = destinationPoint
                icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_start)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "목적지"
            }
            mapView.overlays.add(endMarker)

            mapView.controller.setZoom(17.0)
            mapView.controller.setCenter(originPoint)
            mapView.invalidate()
        }

        // 🪑 벤치 마커 (bench 모드일 때만)
        if (key == "bench") {
            route.benches.forEachIndexed { index, bench ->
                if (bench.size >= 2) {
                    val benchMarker = Marker(mapView).apply {
                        position = GeoPoint(bench[0], bench[1])
                        icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_bench)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "벤치 #${index + 1}"
                    }
                    mapView.overlays.add(benchMarker)
                }
            }
        }
    }




    /**
     * 경로 종류별 색상을 반환
     */
    private fun getColorByType(type: String): Int {
        return when (type) {
            "shortest"  -> 0xFF1E90FF.toInt() // 파란색
            "safe_path" -> 0xFFFF0000.toInt() // 빨간색
            "bench"     -> 0xFF00FF00.toInt() // 초록색
            else        -> 0xFF555555.toInt() // 회색
        }
    }

    /**
     * 바로 전화를 걸 때 사용
     */
    private fun makeDirectCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }
            1001 -> {
                // CALL_PHONE 권한 결과 처리 (필요 시)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
