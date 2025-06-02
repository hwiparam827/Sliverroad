package com.example.sliverroad.data

data class DeliveryHistoryItem(
    val request_id: String,
    val item_type: String,
    val pickup_location: String,
    val delivery_address: String
)
