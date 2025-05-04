package com.wepool.app.data.model.ride

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.enums.RideDirection

data class Ride(
    val rideId: String = "",
    val driverId: String = "",
    val companyId: String = "",
    val startLocation: GeoPoint = GeoPoint(0.0, 0.0),
    val destination: GeoPoint = GeoPoint(0.0, 0.0),
    val direction: RideDirection? = null, // האם הנסיעה היא TO_WORK או TO_HOME
    val arrivalTime: String? = null,
    val departureTime: String? = null,
    val date: String = "",
    val availableSeats: Int = 0,
    val occupiedSeats : Int = 0,
    val passengers: List<String> = listOf(),
    //val pickupStops: List<GeoPoint> = emptyList(),
    val pickupStops: List<PickupStop> = emptyList(),
    val maxDetourMinutes: Int = 10,
    val currentDetourMinutes: Int = 0,
    val encodedPolyline: String = "",
    val isRecurring: Boolean = false,
    val notes: String?=null
)