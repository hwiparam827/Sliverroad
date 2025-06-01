package com.example.sliverroad.data

data class LoginRequest(
    val id: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val access_token: String,
    val refresh_token: String
)
