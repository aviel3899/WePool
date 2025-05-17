package com.wepool.app.data.model.logic

import com.wepool.app.data.model.ride.PickupStop

data class DurationAndRoute(
    val durationMinutes: Int = 0,
    val encodedPolyline: String = "",
    val pickupTimes: Map<String, String> = emptyMap(),    // נוסע -> זמן איסוף
    val dropoffTimes: Map<String, String> = emptyMap(),  // נוסע -> זמן הורדה
    val orderedStops: List<PickupStop> = emptyList()
)