package com.example.sliverroad.data


import org.json.JSONObject

data class CallRequest(
    val id: String,
    val fare: Int,
    val pickup: String,
    val dropoff: String
)



