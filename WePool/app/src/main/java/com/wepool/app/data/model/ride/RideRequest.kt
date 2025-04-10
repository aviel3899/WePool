package com.wepool.app.data.model.ride

import com.google.firebase.Timestamp
import com.wepool.app.data.model.enums.RequestStatus

data class RideRequest(
    val requestId: String,                      // מזהה ייחודי של הבקשה
    val userId: String = "",                    // UID של המשתמש
    val rideId: String = "",                    // מזהה נסיעה
    val status: RequestStatus = RequestStatus.PENDING, // סטטוס הבקשה
    val requestedAt: Timestamp = Timestamp.now()       // מועד שליחת הבקשה
)
