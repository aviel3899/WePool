package com.wepool.app.data.model.ride

import com.google.firebase.Timestamp

data class RideHistory(
    val userId: String = "",                    // UID של המשתמש
    val rideId: String = "",                    // מזהה הנסיעה
    val date: Timestamp = Timestamp.now(),     // מועד הנסיעה
    val wasDriver: Boolean = false              // האם המשתמש היה נהג
)