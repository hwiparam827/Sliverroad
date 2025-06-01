package com.example.sliverroad.data

data class WorkStatusResponse(
    val is_working: Boolean,
    val can_receive_call: Boolean,
    val work_start: String?,
    val work_end: String?,
    val status_text: String
)
