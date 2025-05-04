package com.wepool.app.data.model.logic

data class DurationAndRoute(
    val durationMinutes: Int,
    val encodedPolyline: String,
    val pickupTimes: Map<String, String> = emptyMap(),    // נוסע -> זמן איסוף
    val dropoffTimes: Map<String, String> = emptyMap()    // נוסע -> זמן הורדה
)