package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.users.Passenger

interface IPassengerRepository {
    suspend fun createOrUpdatePassenger(uid: String, passenger: Passenger)
    suspend fun getPassenger(uid: String): Passenger?
}