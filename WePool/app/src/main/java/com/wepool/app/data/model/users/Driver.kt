package com.wepool.app.data.model.users

import com.wepool.app.data.model.users.User

data class Driver(
    val user: User = User(),
    val vehicleDetails: String = "", // תיאור הרכב לדוגמה: "Toyota Corolla 2019"
    val activeRideId: String? = null // אם הנהג פרסם נסיעה – זה מזהה הנסיעה הפעילה
)