package com.example.sliverroad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class OsmMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var clientPhoneNumber: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var myLocationMarker: Marker
    private var locationRequest: LocationRequest? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.osm_map)
        mapView.setMultiTouchControls(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        myLocationMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_location)
        }
        mapView.overlays.add(myLocationMarker)

        // 💡 이 부분을 onCreate() 안으로 옮기기
        val btnStartNavigation = findViewById<ImageButton>(R.id.btnStartNavigation)
        val navigationOverlay = findViewById<LinearLayout>(R.id.navigationOverlay)
        val pathSelectionBar = findViewById<LinearLayout>(R.id.pathSelectionBar)
        val btnCallClient = findViewById<ImageButton>(R.id.btnCallClient)
        val btnComplete = findViewById<ImageButton>(R.id.btnComplete)
        val headerCard = findViewById<LinearLayout>(R.id.headerCard)
        val headerCard2 = findViewById<LinearLayout>(R.id.headerCard2)

        checkLocationPermissionAndStartUpdates()

        clientPhoneNumber = intent.getStringExtra("clientPhone") ?: "01012345678"

        btnStartNavigation.setOnClickListener {
            btnStartNavigation.visibility = android.view.View.GONE
            pathSelectionBar.visibility = android.view.View.GONE
            headerCard.visibility = android.view.View.GONE
            navigationOverlay.visibility = android.view.View.VISIBLE
            headerCard2.visibility = android.view.View.VISIBLE

        }

        btnCallClient.setOnClickListener {
            makeDirectCall(clientPhoneNumber)
        }

        btnComplete.setOnClickListener {
            val intent = Intent(this, DeliveryMapActivity::class.java)
            startActivity(intent)
            finish()
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

    private fun checkLocationPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1000)
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val geoPoint = GeoPoint(location.latitude, location.longitude)

                myLocationMarker.position = geoPoint
                //mapView.controller.setCenter(geoPoint)
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

    private fun drawRouteFromFile(fileName: String) {
        mapView.overlays.removeAll { it is Polyline }

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
            // 출발점 마커
            val startMarker = Marker(mapView).apply {
                position = allGeoPoints.first()
                icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_start)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "출발지"
            }
            // 도착점 마커
            val endMarker = Marker(mapView).apply {
                position = allGeoPoints.last()
                icon = ContextCompat.getDrawable(this@OsmMapActivity, R.drawable.ic_start)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "도착지"
            }
            mapView.overlays.add(polyline)
            mapView.controller.setZoom(20.0)
            mapView.controller.setCenter(allGeoPoints.first())
            mapView.invalidate()
            mapView.overlays.add(startMarker)
            mapView.overlays.add(endMarker)

        }

        // 위치 마커는 다시 추가
        mapView.overlays.add(myLocationMarker)
    }

    private fun getColorByType(type: String): Int {
        return when (type) {
            "shortest" -> 0xFF1E90FF.toInt()
            "safe" -> 0xFFFF0000.toInt()
            "bench" -> 0xFF00FF00.toInt()
            else -> 0xFF555555.toInt()
        }
    }

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
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    makeDirectCall(clientPhoneNumber)
                } else {
                    Toast.makeText(this, "전화 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
