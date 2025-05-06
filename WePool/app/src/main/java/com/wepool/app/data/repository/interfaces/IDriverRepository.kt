package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.Driver

interface IDriverRepository {
    suspend fun saveDriver(driver: Driver)
    suspend fun getDriver(uid: String): Driver?
    suspend fun deleteDriver(uid: String)
    suspend fun updateVehicleDetails(uid: String, vehicleDetails: String)

    suspend fun addActiveRideToDriver(driverId: String, rideId: String)
    suspend fun removeActiveRideFromDriver(driverId: String, rideId: String)
    suspend fun getActiveRidesForDriver(driverId: String): List<Ride>
}

