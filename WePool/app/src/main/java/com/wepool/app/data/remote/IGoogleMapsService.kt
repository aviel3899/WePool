package com.wepool.app.data.remote

import com.google.firebase.firestore.GeoPoint

interface IGoogleMapsService {

    /**
     * מחשב את משך זמן הנסיעה בדקות מ־origin ל־destination,
     * בהתבסס על arrivalTime בפורמט "HH:mm"
     */
    suspend fun getDurationFromGoogleApi(
        origin: GeoPoint,
        destination: GeoPoint,
        arrivalTime: String
    ): Int
}