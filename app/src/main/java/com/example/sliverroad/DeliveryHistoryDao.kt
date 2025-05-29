package com.example.sliverroad.data

/**
 * 나중에 Room @Dao 인터페이스로 교체하세요.
 */
interface DeliveryHistoryDao {
    suspend fun findByDate(date: String): List<DeliveryHistory>
}
