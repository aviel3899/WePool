package com.wepool.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.repository.interfaces.ILocationDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class LocationDataRepository(
    private val firestore: FirebaseFirestore
) : ILocationDataRepository {

    private fun userLocationsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("savedLocations")

    override suspend fun getLocationsForUser(uid: String): List<LocationData> = withContext(Dispatchers.IO) {
        val snapshot = userLocationsCollection(uid).get().await()
        return@withContext snapshot.toObjects(LocationData::class.java)
    }

    override suspend fun addLocationToUser(uid: String, location: LocationData)  {
        userLocationsCollection(uid)
            .document(location.name)
            .set(location)
            .await()
    }

    override suspend fun deleteLocationFromUser(uid: String, locationName: String)  {
        userLocationsCollection(uid)
            .document(locationName)
            .delete()
            .await()
    }

    override suspend fun getLocationByPlaceId(uid: String, placeId: String): LocationData? = withContext(Dispatchers.IO) {
        val snapshot = userLocationsCollection(uid)
            .whereEqualTo("placeId", placeId)
            .limit(1)
            .get()
            .await()

        return@withContext snapshot.documents.firstOrNull()?.toObject(LocationData::class.java)
    }
}