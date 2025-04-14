package com.wepool.app.data.model.users

import com.google.firebase.firestore.GeoPoint

data class Passenger(
    val user: User,                             // מופע בסיסי של המשתמש
    val preferredPickupLocation: GeoPoint? = null, // מיקום איסוף מועדף (אם הוגדר)
    val preferredDepartureTime: String = "" // זמן יציאה מועדף
)