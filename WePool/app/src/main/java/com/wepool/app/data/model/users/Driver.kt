package com.wepool.app.data.model.users

data class Driver(
    val user: User = User(),
    val vehicleDetails: String = "", // תיאור הרכב לדוגמה: "Toyota Corolla 2019"
    val activeRideId: List<String> = emptyList() // אם הנהג פרסם נסיעה – זה מזהה הנסיעה הפעילה
)