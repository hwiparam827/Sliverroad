// com/example/sliverroad/model/RouteModels.kt
package com.example.sliverroad.model

/**
 * API가 반환하는 JSON 전체를 매핑하기 위한 최상위 데이터 클래스
 */
data class FindRouteResponse(
    val request_id: String,
    val requester_phone: String,
    val routes: Map<String, IndividualRoute>,  // "shortest", "safe_path", "less_stairs", "bench", 그리고 "pickup"/"delivery"
    val origin: LatLng,
    val destination: LatLng,
    val destination_type: String
)

/**
 * routes 맵의 각 경로별 정보 (“pickup” 등)
 */
data class IndividualRoute(
    val walk_route: WalkRoute,
    val transit_route: List<TransitSegment>,
    val total_distance: Double,
    val total_risk: Double,
    val total_width: Double,
    val benches: List<List<Double>>  , // [[lat, lng], [lat, lng], ...]
    val elevator_info: List<List<Double>> ,
)

data class WalkRoute(
    val coordinates: List<List<Double>> // [[위도, 경도], [위도, 경도], ...]
)

data class TransitSegment(
    val line: String?,
    val vehicle: String,
    val departure_station: String,
    val arrival_station: String,
    val num_stations: Int,
    val coordinates: List<List<Double>>
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)
