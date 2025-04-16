package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.common.LocationData

interface ILocationDataRepository {

    suspend fun getLocationsForUser(uid: String): List<LocationData>
    suspend fun addLocationToUser(uid: String, location: LocationData)
    suspend fun deleteLocationFromUser(uid: String, locationName: String)
    suspend fun getLocationByPlaceId(uid: String, placeId: String): LocationData?
}
