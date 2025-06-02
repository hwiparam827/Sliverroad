package com.example.sliverroad.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// (나중에 RoomDatabase 상속으로 바꿀 때까지는 이렇게 더미 구현)
class AppDatabase private constructor() {

    fun deliveryHistoryDao(): DeliveryHistoryDao =
        object : DeliveryHistoryDao {
            override suspend fun findByDate(date: String): List<DeliveryHistory> =
                emptyList()
        }


    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                AppDatabase().also { INSTANCE = it }
            }
    }
}
