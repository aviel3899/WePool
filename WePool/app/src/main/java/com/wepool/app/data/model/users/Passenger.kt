package com.wepool.app.data.model.users

data class Passenger(
    val user: User = User(),
    val activeRideId: List<String> = emptyList()
)