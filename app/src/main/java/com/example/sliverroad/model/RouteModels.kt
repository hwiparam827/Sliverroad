package com.example.sliverroad.model

data class TransitStep(
    val line: String?,
    val vehicle: String,
    val departureStation: String,
    val arrivalStation: String,
    val numStations: Int,
    val coordinates: List<Pair<Double, Double>>  // List of (lat, lng)
)

data class WalkRoute(
    val coordinates: List<Pair<Double, Double>>  // List of (lat, lng)
)

data class IndividualRoute(
    val walkRoute: WalkRoute,
    val transitRoute: List<TransitStep>,
    val totalDistance: Double,
    val totalRisk: Double,
    val totalWidth: Double,
    val benches: List<Pair<Double, Double>>      // List of bench coordinates (lat, lng)
)

/**
 * 최종적으로 "shortest", "safe_path", "less_stairs", "bench" 등의 키에
 * 해당 IndividualRoute를 매핑하기 위한 타입입니다.
 */
typealias RouteData = Map<String, IndividualRoute>
