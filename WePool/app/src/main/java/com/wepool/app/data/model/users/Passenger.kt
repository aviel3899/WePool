package com.wepool.app.data.model.users

import com.wepool.app.data.model.common.LocationData

data class Passenger(
    val user: User = User(),
    val favoriteLocations: List<LocationData> = emptyList()
    // val preferredPickupLocation: GeoPoint? = null,
    // val preferredArrivalTime: String = ""
)