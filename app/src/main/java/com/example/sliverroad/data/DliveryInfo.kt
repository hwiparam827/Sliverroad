package com.example.sliverroad.data

import com.google.gson.annotations.SerializedName

data class Driver(
    val name: String,
    val phone: String,
    val email: String?, // null 허용
    val address: String,
    @SerializedName("profile_image")
    val profileImage: String
)
