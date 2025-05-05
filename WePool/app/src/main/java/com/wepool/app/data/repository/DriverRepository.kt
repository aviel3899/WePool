package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.Driver
import com.wepool.app.data.repository.interfaces.IDriverRepository
import kotlinx.coroutines.tasks.await

class DriverRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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

    override suspend fun addActiveRideToDriver(driverId: String, rideId: String) {
        val driverRef = firestore
            .collection("users")
            .document(driverId)
            .collection("driverData")
            .document("info")

        val snapshot = driverRef.get().await()
        val driver = snapshot.toObject(Driver::class.java)

        if (driver != null) {
            val updatedList = driver.activeRideId.toMutableSet().apply { add(rideId) }.toList()
            driverRef.update("activeRideIds", updatedList).await()
            Log.d("DriverUpdate", "✅ נוספה נסיעה פעילה לנהג ($rideId)")
        } else {
            Log.w("DriverUpdate", "⚠ לא נמצא נהג עם UID: $driverId")
        }
    }

    override suspend fun removeActiveRideFromDriver(driverId: String, rideId: String) {
        val driverRef = firestore
            .collection("users")
            .document(driverId)
            .collection("driverData")
            .document("info")

        val snapshot = driverRef.get().await()
        val driver = snapshot.toObject(Driver::class.java)

        if (driver != null) {
            val updatedList = driver.activeRideId.toMutableList().apply { remove(rideId) }
            driverRef.update("activeRideIds", updatedList).await()
            Log.d("DriverUpdate", "🗑️ הנסיעה $rideId הוסרה מ־activeRideIds של הנהג")
        } else {
            Log.w("DriverUpdate", "⚠ לא נמצא נהג עם UID: $driverId")
        }
    }

    override suspend fun getActiveRidesForDriver(driverId: String): List<Ride> {
        return try {
            val snapshot = firestore.collection("rides")
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val rides = snapshot.toObjects(Ride::class.java)
            Log.d("DriverRepo", "✅ נמצאו ${rides.size} נסיעות פעילות לנהג $driverId")
            rides
        } catch (e: Exception) {
            Log.e("DriverRepo", "❌ שגיאה בשליפת נסיעות פעילות לנהג: ${e.message}", e)
            emptyList()
        }
    }


}
