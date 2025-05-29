package com.example.sliverroad.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// CallRequestDao.kt
@Dao
interface CallRequestDao {
    @Query("SELECT * FROM call_requests WHERE id = :id")
    suspend fun getById(id: Int): CallRequest?

    // 기존 함수들…
    @Query("SELECT * FROM call_requests WHERE handled = 0 ORDER BY ((lat - :myLat)*(lat - :myLat) + (lng - :myLng)*(lng - :myLng)) LIMIT 1")
    fun findNearestUnHandled(myLat: Double, myLng: Double): Flow<CallRequest?>

    @Update
    suspend fun update(request: CallRequest)
}
