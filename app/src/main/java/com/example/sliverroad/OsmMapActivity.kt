package com.example.sliverroad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class OsmMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var clientPhoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.osm_map)
        mapView.setMultiTouchControls(true)

        val btnStartNavigation = findViewById<ImageButton>(R.id.btnStartNavigation)
        val navigationOverlay = findViewById<LinearLayout>(R.id.navigationOverlay)
        val pathSelectionBar = findViewById<LinearLayout>(R.id.pathSelectionBar)
        val btnCallClient = findViewById<ImageButton>(R.id.btnCallClient)
        val btnComplete = findViewById<ImageButton>(R.id.btnComplete)

        clientPhoneNumber = intent.getStringExtra("clientPhone") ?: "01012345678"

        btnStartNavigation.setOnClickListener {
            btnStartNavigation.visibility = android.view.View.GONE
            pathSelectionBar.visibility = android.view.View.GONE
            navigationOverlay.visibility = android.view.View.VISIBLE
        }

        btnCallClient.setOnClickListener {
            makeDirectCall(clientPhoneNumber)
        }

        btnComplete.setOnClickListener {
            val intent = Intent(this, DeliveryMapActivity::class.java)
            startActivity(intent)
            finish() // 현재 OsmMapActivity 종료
        }


        findViewById<ImageButton>(R.id.btnSafe).setOnClickListener {
            drawRouteFromFile("safe.json")
        }
        findViewById<ImageButton>(R.id.btnShortest).setOnClickListener {
            drawRouteFromFile("shortest.json")
        }
        findViewById<ImageButton>(R.id.btnBench).setOnClickListener {
            drawRouteFromFile("bench.json")
        }

        drawRouteFromFile("shortest.json")
    }

    private fun drawRouteFromFile(fileName: String) {
        mapView.overlayManager.clear()

        val json = assets.open(fileName).bufferedReader().use { it.readText() }
        val routeList: List<Route> = Gson().fromJson(json, object : TypeToken<List<Route>>() {}.type)

        val allGeoPoints = routeList.flatMap { route ->
            route.coordinates.map { GeoPoint(it[0], it[1]) }
        }

        if (allGeoPoints.isNotEmpty()) {
            val polyline = Polyline().apply {
                setColor(getColorByType(routeList.first().type))
                width = 8.0f
                setPoints(allGeoPoints)
            }

            mapView.overlayManager.add(polyline)
            mapView.controller.setZoom(19.0)
            mapView.controller.setCenter(allGeoPoints.first())
            mapView.invalidate()
        }
    }

    private fun getColorByType(type: String): Int {
        return when (type) {
            "shortest" -> 0xFF1E90FF.toInt()
            "safe" -> 0xFF2ECC71.toInt()
            "bench" -> 0xFF555555.toInt()
            else -> 0xFF555555.toInt()
        }
    }

    // ✅ 전화 걸기 함수
    private fun makeDirectCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1001)
        }
    }

    // ✅ 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            makeDirectCall(clientPhoneNumber)
        } else {
            Toast.makeText(this, "전화 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
