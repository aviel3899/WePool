package com.wepool.app.data.model.ride

import com.wepool.app.data.model.common.LocationData

data class PickupStop(
    val location: LocationData = LocationData(),
    val passengerId: String = "",
    val pickupTime: String? = null,    // שעת איסוף(HH:mm)
    val dropoffTime: String? = null    // שעת הורדה(HH:mm)
)