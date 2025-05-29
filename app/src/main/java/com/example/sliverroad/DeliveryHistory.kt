package com.example.sliverroad.data

/**
 * 배송 내역 한 건을 나타내는 데이터 클래스.
 * 나중에 Room @Entity 어노테이션을 붙이거나, 실제 필드를 바꿔주시면 됩니다.
 */
data class DeliveryHistory(
    val id: Long = 0L,
    val date: String = "",
    val fromLocation: String = "",
    val toLocation: String = "",
    val packageType: String = "",
    val fee: Int = 0
)
