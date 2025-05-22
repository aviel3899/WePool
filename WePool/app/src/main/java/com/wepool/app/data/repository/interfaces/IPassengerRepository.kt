package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.Passenger

interface IPassengerRepository {

    suspend fun getPassenger(uid: String): Passenger?
    suspend fun savePassengerData(uid: String, passenger: Passenger)
    suspend fun deletePassenger(uid:String)
    suspend fun addActiveRideToPassenger(passengerId: String, rideId: String)
    suspend fun removeActiveRideFromPassenger(passengerId: String, rideId: String)
    suspend fun getActiveRidesForPassenger(passengerId: String): List<Ride>

    //suspend fun updatePreferredPickupLocation(uid: String, location: GeoPoint)
    //suspend fun updatePreferredArrivalTime(uid: String, arrivalTime: String)
}
