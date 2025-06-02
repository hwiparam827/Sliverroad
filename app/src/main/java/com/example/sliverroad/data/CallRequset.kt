package com.example.sliverroad.data

import java.io.Serializable

data class CallRequest(
    val id: Int,
    val request_id: String,
    val fare: Int,
    val pickup: String,
    val dropoff: String,
) : Serializable  // ✅ 추가