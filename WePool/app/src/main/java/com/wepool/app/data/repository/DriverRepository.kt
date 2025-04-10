package com.wepool.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.users.Driver
import kotlinx.coroutines.tasks.await

class DriverRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createDriver(driver: Driver) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("drivers").document(uid).set(driver).await()
    }

    suspend fun getDriver(): Driver? {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val snapshot = firestore.collection("drivers").document(uid).get().await()
        return snapshot.toObject(Driver::class.java)
    }

    suspend fun deleteDriver() {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("drivers").document(uid).delete().await()
    }

    private suspend fun updateDriverFields(updates: Map<String, Any>) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("drivers").document(uid).update(updates).await()
    }

    suspend fun updateAvailableSeats(seats: Int) {
        updateDriverFields(mapOf("availableSeats" to seats))
    }

    suspend fun updateVehicleDetails(vehicleDetails: String) {
        updateDriverFields(mapOf("vehicleDetails" to vehicleDetails))
    }

    suspend fun updateMaxDetourMinutes(maxDetour: Int) {
        updateDriverFields(mapOf("maxDetourMinutes" to maxDetour))
    }

}