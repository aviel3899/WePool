package com.wepool.app.data.model.ride

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Ride(
    val rideId: String,                         // מזהה ייחודי של נסיעה
    val driverId: String = "",                  // UID של הנהג
    val companyId: String = "",                 // מזהה החברה
    val startLocation: GeoPoint = GeoPoint(0.0, 0.0), // נקודת התחלה
    val destination: GeoPoint = GeoPoint(0.0, 0.0),   // יעד
    val departureTime: Timestamp = Timestamp.now(),  // זמן יציאה
    val availableSeats: Int = 0,                // מקומות פנויים
    val passengers: List<String> = listOf(),    // UID של נוסעים
    val isRecurring: Boolean = false,           // האם זו נסיעה קבועה?
    val notes: String? = null,                  // הערות כלליות
    val maxDetourMinutes: Int = 10              // סטייה מקסימלית לנסיעה הזו
)
