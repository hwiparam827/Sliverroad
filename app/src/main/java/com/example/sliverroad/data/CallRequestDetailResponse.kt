
package com.example.sliverroad.data

// 서버 JSON 예시에 맞춘 데이터 클래스입니다.
data class CallRequestDetailResponse(
    val id: String,
    val requester_name: String,
    val requester_phone: String,
    val delivery_person_name: String?,   // null 가능
    val delivery_person_phone: String?,  // null 가능
    val recipient_name: String,
    val recipient_phone: String,
    val pickup_time: String,
    val delivery_time: String?,          // null 가능
    val status: String,
    val pickup_location: String,
    val item_type: String,
    val item_name: String,
    val item_weight: Double,
    val item_value: Double,
    val instructions: String,
    val photos: Photos,
    val address: String
) {
    data class Photos(
        val pickup: List<String>,
        val delivery: List<String>
    )
}

