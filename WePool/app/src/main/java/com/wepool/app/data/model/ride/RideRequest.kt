package com.wepool.app.data.model.ride

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.RequestStatus

data class RideRequest(
    val requestId: String = "",
    val rideId: String = "",
    val passengerId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    //val pickupLocation: GeoPoint? = null,
    val pickupLocation: LocationData = LocationData(),
    val timestamp: Long = System.currentTimeMillis()
)