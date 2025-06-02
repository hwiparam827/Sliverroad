package com.example.sliverroad.data

data class RouteResponse(
    val request_id: String,
    val requester_phone: String,
    val routes: Map<String, RouteDetail>
)

data class RouteDetail(
    val walk_route: WalkRoute,
    val transit_route: List<TransitStep>,
    val total_distance: Double,
    val total_risk: Double,
    val total_width: Double,
    val benches: List<List<Double>>
)

data class WalkRoute(
    val coordinates: List<List<Double>>
)

data class TransitStep(
    val line: String?, // null 가능
    val vehicle: String,
    val departure_station: String,
    val arrival_station: String,
    val num_stations: Int,
    val coordinates: List<List<Double>>
)
