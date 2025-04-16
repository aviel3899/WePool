package com.wepool.app.data.repository

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.users.Passenger

interface IPassengerRepository {

    suspend fun getPassenger(uid: String): Passenger?
    suspend fun updatePreferredPickupLocation(uid: String, location: GeoPoint)
    suspend fun updatePreferredArrivalTime(uid: String, departureTime: String)
    suspend fun savePassengerData(uid: String, passenger: Passenger)
}
