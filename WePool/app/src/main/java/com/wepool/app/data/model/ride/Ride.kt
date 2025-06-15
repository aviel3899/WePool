package com.wepool.app.data.model.ride

import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.logic.DepartureCalculationResult

data class Ride(
    val rideId: String = "",
    val driverId: String = "",
    val companyCode: String = "",
    val startLocation: LocationData = LocationData(),
    val destination: LocationData = LocationData(),
    val direction: RideDirection? = null, // האם הנסיעה היא TO_WORK או TO_HOME
    val arrivalTime: String? = null,
    val departureTime: String? = null,
    val date: String = "",
    val availableSeats: Int = 0,
    val occupiedSeats : Int = 0,
    val passengers: List<String> = listOf(),
    val pickupStops: List<PickupStop> = emptyList(),
    val maxDetourMinutes: Int = 10,
    val currentDetourMinutes: Int = 0,
    val encodedPolyline: String = "",
    val originalRoute: DepartureCalculationResult = DepartureCalculationResult(),
    val active: Boolean = true,
    val notes: String?=null
)
