package com.wepool.app.data.model.ride

data class Rating(
    val rideId: String = "",                    // מזהה נסיעה
    val fromUserId: String = "",                // מי נתן את הדירוג
    val toUserId: String = "",                  // למי ניתן הדירוג
    val score: Int = 0,                         // ציון (1–5)
    val comment: String? = null                 // הערה חופשית
)
