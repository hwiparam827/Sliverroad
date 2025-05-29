package com.example.sliverroad  // ← 프로젝트 패키지에 맞게 변경

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Route(
    val type: String,
    val coordinates: List<List<Double>>
)

fun loadRouteData(context: Context): List<Route> {
    val json = context.assets.open("route_data.json").bufferedReader().use { it.readText() }
    val typeToken = object : TypeToken<List<Route>>() {}.type
    return Gson().fromJson(json, typeToken)
}
