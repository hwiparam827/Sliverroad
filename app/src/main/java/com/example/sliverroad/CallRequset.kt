package com.example.sliverroad.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_requests")
data class CallRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fare: Int,
    val pickup: String,
    val dropoff: String,
    val lat: Double,
    val lng: Double,
    val handled: Boolean = false
)
