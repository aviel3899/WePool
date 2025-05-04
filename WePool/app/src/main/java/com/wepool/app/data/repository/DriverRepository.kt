package com.wepool.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.users.Driver
import com.wepool.app.data.repository.interfaces.IDriverRepository
import kotlinx.coroutines.tasks.await

class DriverRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : IDriverRepository {

    override suspend fun saveDriver(driver: Driver) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .set(driver)
            .await()
    }

    override suspend fun getDriver(uid: String): Driver? {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .get()
            .await()
        return snapshot.toObject(Driver::class.java)
    }

    override suspend fun deleteDriver(uid: String) {
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .delete()
            .await()
    }

    override suspend fun updateVehicleDetails(uid: String, vehicleDetails: String) {
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .update("vehicleDetails", vehicleDetails)
            .await()
    }

    override suspend fun updateActiveRideIds(uid: String, rideIds: List<String>) {
        firestore.collection("users")
            .document(uid)
            .collection("driverData")
            .document("info")
            .update("activeRideIds", rideIds)
            .await()
    }
}
