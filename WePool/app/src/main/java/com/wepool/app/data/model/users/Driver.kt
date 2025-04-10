package com.wepool.app.data.model.users

data class Driver(
    val user: User,
    val availableSeats: Int = 0,
    val vehicleDetails: String = "",
    val maxDetourMinutes: Int = 10
)