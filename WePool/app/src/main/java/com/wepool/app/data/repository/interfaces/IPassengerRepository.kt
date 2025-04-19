package com.wepool.app.data.repository.interfaces

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.users.Passenger

interface IPassengerRepository {

    suspend fun getPassenger(uid: String): Passenger?
    suspend fun savePassengerData(uid: String, passenger: Passenger)
    suspend fun addFavoriteLocation(uid: String, location: LocationData)
    suspend fun removeFavoriteLocation(uid: String, placeId: String)
    //suspend fun updatePreferredPickupLocation(uid: String, location: GeoPoint)
  //  suspend fun updatePreferredArrivalTime(uid: String, arrivalTime: String)

}
