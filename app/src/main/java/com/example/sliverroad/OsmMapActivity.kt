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
import android.widget.TextView
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
        val btnFindBench = findViewById<ImageButton>(R.id.btnFindBench)

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
            btnFindBench.visibility = android.view.View.VISIBLE

        }


        // 9) “의뢰인에게 전화 걸기” 버튼
        btnCallClient.setOnClickListener {
            makeDirectCall(clientPhoneNumber)
        }

        btnFindBench.setOnClickListener {
            val intent = Intent(this@OsmMapActivity, FindBenchActivity::class.java)
            startActivity(intent)  // ← 반드시 apply 블록 바깥에서 호출해야 함
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
                ).enqueue(object : Callback<FindRouteResponse> {
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
                            Log.d(
                                "Route",
                                "origin = ${data.origin.latitude}, ${data.origin.longitude}"
                            )
                            Log.d(
                                "Route",
                                "destination = ${data.destination.latitude}, ${data.destination.longitude}"
                            )
                            Log.d("Route", "destination_type = ${data.destination_type}")

                            // 2) 네 가지 경로 중에서 “shortest” 경로 정보 꺼내기
                            val deliveryinfo = data.routes["delivery"]
                            if (deliveryinfo != null) {
                                // 보행 좌표, 벤치 좌표 등은 pickupInfo를 통해 읽어야 합니다.
                                val walkCoords: List<List<Double>> =
                                    deliveryinfo.walk_route.coordinates
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
                            val jsonAllRoutes =
                                gson.toJson(data.routes)    // Map<String, RouteInfo> 전체를 JSON string으로
                            val originJson = gson.toJson(data.origin)
                            val destinationJson = gson.toJson(data.destination)


                            val intent =
                                Intent(this@OsmMapActivity, DeliveryMapActivity::class.java).apply {
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
                            Log.e(
                                "API",
                                "findRoute 실패: HTTP ${response.code()} / ${
                                    response.errorBody()?.string()
                                }"
                            )
                            Toast.makeText(
                                this@OsmMapActivity,
                                "경로 조회에 실패했습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<FindRouteResponse>, t: Throwable) {
                        Log.e("API", "findRoute 네트워크 오류", t)
                        Toast.makeText(this@OsmMapActivity, "경로 조회 네트워크 오류", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            }

        }


        //    CallInfoActivity에서 Intent.putExtra("routes_json", gson.toJson(data.routes)) 했다 가정
        // 11) server에서 넘어온 routes_json 파싱
        val routesJson = intent.getStringExtra("routes_json") ?: "{}"
        Log.d("OsmMap", "▶▶▶ routes_json raw = $routesJson")
        allRoutes = Gson().fromJson(
            routesJson,
            object : TypeToken<Map<String, IndividualRoute>>() {}.type
        )
        Log.d("OsmMap", "▶▶▶ allRoutes keySet = ${allRoutes.keys}")

        // 12) 버튼별로, 해당 키(key)에 해당하는 IndividualRoute를 꺼내서 그리기
        btnSafe.setOnClickListener {
            drawRouteFromData("safe_path")
        }
        btnShortest.setOnClickListener {
            drawRouteFromData("shortest")
        }
        btnBench.setOnClickListener {
            drawRouteFromData("bench")
        }

        // 앱 실행 시 기본으로 “shortest” 경로를 한 번 그려둠
        drawRouteFromData("shortest")
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
        val inputStream = resources.openRawResource(R.raw.sliver_load)
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
    private fun compareWithShortestRoute(selectedKey: String) {
        val shortest = allRoutes["shortest"]
        val selected = allRoutes[selectedKey]

        if (shortest == null || selected == null) {
            Toast.makeText(this, "경로 비교에 필요한 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 차이 계산
        val distanceDiff = selected.total_distance - shortest.total_distance
        val riskDiff = selected.total_risk - shortest.total_risk
        val widthDiff = selected.total_width - shortest.total_width
        val benchDiff = selected.benches.size - shortest.benches.size

        // 포맷팅
        val text = """
        🚩 경로 비교 (${selectedKey})
        - 거리: ${"%.1f".format(selected.total_distance)}m (${if (distanceDiff >= 0) "+" else ""}${"%.1f".format(distanceDiff)}m)
        - 위험도: ${"%.1f".format(selected.total_risk)} (${if (riskDiff >= 0) "+" else ""}${"%.1f".format(riskDiff)})
        - 도로 폭: ${"%.1f".format(selected.total_width)} (${if (widthDiff >= 0) "+" else ""}${"%.1f".format(widthDiff)})
        - 벤치 수: ${selected.benches.size} (${if (benchDiff >= 0) "+" else ""}$benchDiff)
    """.trimIndent()

        // 표시
        val tvComparison = findViewById<TextView>(R.id.tvRouteComparison)
        tvComparison.text = text
        tvComparison.visibility = android.view.View.VISIBLE
    }

    /**
     * 서버에서 받아온 allRoutes 맵에서 key에 대응하는 IndividualRoute를 꺼내서 지도에 그립니다.
     * key: "shortest" or "safe_path" or "bench"
     */
    private fun drawRouteFromData(key: String) {
        // 기존 Polyline을 모두 제거 (단, 내 위치 마커만 남겨두기 위해 잠시 꺼냄)
        mapView.overlays.removeAll { it is Polyline || it == myLocationMarker }
        Log.d("OsmMap", "allRoutes keys = ${allRoutes.keys}")

        // Map<String, IndividualRoute> 에서 해당 key 추출
        val route: IndividualRoute? = allRoutes[key]
        if (route == null) {
            Toast.makeText(this, "해당 경로('$key') 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) walk_route 좌표 (List<List<Double>> 형태)
        val walkCoords: List<GeoPoint> = route.walk_route.coordinates.map { coord ->
            GeoPoint(coord[0], coord[1])
        }

        // 2) transit_route 내의 각 세그먼트(segment) 좌표들을 모두 꺼내서 GeoPoint 리스트로 변환
        val transitCoords: List<GeoPoint> = route.transit_route.flatMap { segment ->
            // segment.coordinates: List<List<Double>>
            segment.coordinates.map { coord ->
                GeoPoint(coord[0], coord[1])
            }

        }
        // ✅ 예외 처리: 두 경로가 모두 비어 있으면 그리지 않음
        if (walkCoords.isEmpty() && transitCoords.isEmpty()) {
            Toast.makeText(this, "해당 경로('$key')는 유효한 좌표가 없어 지도에 표시하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val color = getColorByType(key)
        if (color == null) {
            Toast.makeText(this, "해당 경로('$key')는 허용되지 않은 유형입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3) 워킹 구간 + 트랜짓 구간 좌표를 합친 전체 리스트
        val allGeoPoints: List<GeoPoint> = buildList {
            addAll(walkCoords)
            addAll(transitCoords)
        }

        if (allGeoPoints.isNotEmpty()) {
            // Polyline을 생성하고 색상은 key 에 따라 다르게 설정
            // 이후에 Polyline 생성 시:
            val polyline = Polyline().apply {
                setColor(color)
                width = 11.0f
                setPoints(allGeoPoints)
            }
            mapView.overlays.add(polyline)

            // 출발점 마커
            val startMarker = Marker(mapView).apply {
                position = allGeoPoints.first()
                icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_start)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "출발지"
            }
            mapView.overlays.add(startMarker)

            // 도착점 마커
            val endMarker = Marker(mapView).apply {
                position = allGeoPoints.last()
                icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_start)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "도착지"
            }
            mapView.overlays.add(endMarker)

            // 맵 중앙 및 줌 설정
            mapView.controller.setZoom(20.0)
            mapView.controller.setCenter(allGeoPoints.first())
            mapView.invalidate()
        } else {
            Toast.makeText(this, "경로 좌표가 비어 있습니다.", Toast.LENGTH_SHORT).show()
        }
        // 5) 벤치 마커 표시
        for (coord in route.benches) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(coord[0], coord[1])
                icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_bench)
                title = "벤치"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }

// 6) 엘리베이터 마커 표시
        for (coord in route.elevator_info) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(coord[0], coord[1])
                icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_elevator)
                title = "엘리베이터"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }


        // 4) 마지막으로 내 위치 마커만 다시 꺼내서 추가
        mapView.overlays.add(myLocationMarker)
        compareWithShortestRoute(key)
    }

    /**
     * 경로 종류별 색상을 반환
     */
    private fun getColorByType(type: String): Int? {
        return when (type) {
            "shortest"  -> 0xFF1E90FF.toInt() // 파란색
            "safe_path" -> 0xFFFF0000.toInt() // 빨간색
            "bench"     -> 0xFF00FF00.toInt() // 초록색
            else -> {
                Log.w("getColorByType", "❗알 수 없는 경로 타입: '$type'")
                null // 색상 없음 → 경로 그리지 않음
            }
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
