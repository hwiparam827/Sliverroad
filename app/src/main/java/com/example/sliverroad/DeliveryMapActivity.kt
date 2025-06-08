// OsmMapActivity.kt (최신 수정 통합 버전)
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
import com.example.sliverroad.model.IndividualRoute
import com.example.sliverroad.model.FindRouteResponse
import com.example.sliverroad.model.LatLng
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class DeliveryMapActivity : AppCompatActivity() {

    private lateinit var accessToken: String
    private lateinit var requestId: String
    private lateinit var mapView: MapView
    private lateinit var clientPhoneNumber: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var myLocationMarker: Marker
    private var locationRequest: LocationRequest? = null
    private var assignmentId: Int? = null
    private var allRoutes: Map<String, IndividualRoute> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accessToken = intent.getStringExtra("access_token") ?: ""
        requestId = intent.getStringExtra("request_id") ?: ""
        assignmentId = intent.getIntExtra("assignment_id", -1)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_delivery_maps)

        mapView = findViewById(R.id.osm_map)
        mapView.setMultiTouchControls(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        myLocationMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@DeliveryMapActivity, R.drawable.ic_location)
        }
        mapView.overlays.add(myLocationMarker)

        val btnStartNavigation = findViewById<ImageButton>(R.id.btnStartNavigation)
        val navigationOverlay = findViewById<LinearLayout>(R.id.navigationOverlay)
        val pathSelectionBar = findViewById<LinearLayout>(R.id.pathSelectionBar)
        val btnCallClient = findViewById<ImageButton>(R.id.btnCallClient)
        val btnComplete = findViewById<ImageButton>(R.id.btnComplete2)
        val headerCard = findViewById<LinearLayout>(R.id.headerCard)
        val headerCard2 = findViewById<LinearLayout>(R.id.headerCard2)
        val btnSafe = findViewById<ImageButton>(R.id.btnSafe)
        val btnShortest = findViewById<ImageButton>(R.id.btnShortest)
        val btnBench = findViewById<ImageButton>(R.id.btnBench)
        val btnFindBench = findViewById<ImageButton>(R.id.btnFindBench2)


        checkLocationPermissionAndStartUpdates()
        clientPhoneNumber = intent.getStringExtra("clientPhone") ?: ""

        btnStartNavigation.setOnClickListener {
            btnStartNavigation.visibility = android.view.View.GONE
            pathSelectionBar.visibility = android.view.View.GONE
            headerCard.visibility = android.view.View.GONE
            navigationOverlay.visibility = android.view.View.VISIBLE
            headerCard2.visibility = android.view.View.VISIBLE
            btnComplete.visibility = android.view.View.VISIBLE
            btnFindBench.visibility = android.view.View.VISIBLE

        }

        btnCallClient.setOnClickListener {
            makeDirectCall(clientPhoneNumber)
        }

        // 9) 벤치찾았을때 좌표추가하기 위해 이동하는 코드
        btnFindBench.setOnClickListener {
            val intent = Intent(this@DeliveryMapActivity, FindBenchActivity::class.java)
            startActivity(intent)  // ← 반드시 apply 블록 바깥에서 호출해야 함
        }
        btnComplete.setOnClickListener {

                    val intent = Intent(this@DeliveryMapActivity, DeliveryFinishActivity::class.java).apply {
                        putExtra("access_token", accessToken)
                        putExtra("request_id", requestId)
                        putExtra("assignment_id", assignmentId)
                    }
                    startActivity(intent)
                    btnComplete.visibility = android.view.View.GONE
                    finish()
                        }
        val routesJson = intent.getStringExtra("routes_json") ?: "{}"
        allRoutes = Gson().fromJson(routesJson, object : TypeToken<Map<String, IndividualRoute>>() {}.type)

        btnSafe.setOnClickListener { drawRouteFromData("safe_path") }
        btnShortest.setOnClickListener { drawRouteFromData("shortest") }
        btnBench.setOnClickListener { drawRouteFromData("bench") }

        drawRouteFromData("shortest")

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
                icon = ContextCompat.getDrawable(this@DeliveryMapActivity, R.drawable.ic_start)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "출발지"
            }
            mapView.overlays.add(startMarker)

            // 도착점 마커
            val endMarker = Marker(mapView).apply {
                position = allGeoPoints.last()
                icon = ContextCompat.getDrawable(this@DeliveryMapActivity, R.drawable.ic_start)
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
                icon = ContextCompat.getDrawable(this@DeliveryMapActivity, R.drawable.ic_bench)
                title = "벤치"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }

// 6) 엘리베이터 마커 표시
        for (coord in route.elevator_info) {
            val marker = Marker(mapView).apply {
                position = GeoPoint(coord[0], coord[1])
                icon = ContextCompat.getDrawable(this@DeliveryMapActivity, R.drawable.ic_elevator)
                title = "엘리베이터"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(marker)
        }


        // 4) 마지막으로 내 위치 마커만 다시 꺼내서 추가
        mapView.overlays.add(myLocationMarker)
        compareWithShortestRoute(key)
    }

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


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                myLocationMarker.position = GeoPoint(location.latitude, location.longitude)
                mapView.invalidate()
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest!!, locationCallback, Looper.getMainLooper())
    }

    private fun checkLocationPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
        } else {
            startLocationUpdates()
        }
    }

    private fun makeDirectCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1001)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        mapView.onDetach() // ✅ MapView 리소스 해제
    }
}