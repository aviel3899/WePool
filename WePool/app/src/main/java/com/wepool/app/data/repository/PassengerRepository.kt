package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import kotlinx.coroutines.tasks.await

class PassengerRepository(
    private val firestore: FirebaseFirestore
) : IPassengerRepository {

    override suspend fun getPassenger(uid: String): Passenger? {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("passengerData")
            .document("info")
            .get()
            .await()

        return snapshot.toObject(Passenger::class.java)
    }

    override suspend fun savePassengerData(uid: String, passenger: Passenger)  {
        firestore.collection("users")
            .document(uid)
            .collection("passengerData")
            .document("info")
            .set(passenger)
            .await()
    }

    override suspend fun deletePassenger(uid: String) {
        firestore.collection("users")
            .document(uid)
            .collection("passengerData")
            .document("info")
            .delete()
            .await()
    }

    override suspend fun addActiveRideToPassenger(passengerId: String, rideId: String) {
        val docRef = firestore.collection("users")
            .document(passengerId)
            .collection("passengerData")
            .document("info")

        val snapshot = docRef.get().await()
        val passenger = snapshot.toObject(Passenger::class.java) ?: return

        if (!passenger.activeRideId.contains(rideId)) {
            val updatedList = passenger.activeRideId + rideId
            docRef.update("activeRideId", updatedList).await()
            Log.d("PassengerRepo", "➕ נוספה נסיעה $rideId לנוסע $passengerId")
        }
    }

    override suspend fun removeActiveRideFromPassenger(passengerId: String, rideId: String) {
        val docRef = firestore.collection("users")
            .document(passengerId)
            .collection("passengerData")
            .document("info")

        val snapshot = docRef.get().await()
        val passenger = snapshot.toObject(Passenger::class.java) ?: return

        if (passenger.activeRideId.contains(rideId)) {
            val updatedList = passenger.activeRideId - rideId
            docRef.update("activeRideId", updatedList).await()
            Log.d("PassengerRepo", "➖ הוסרה נסיעה $rideId מנוסע $passengerId")
        }
    }

    override suspend fun getActiveRidesForPassenger(passengerId: String): List<Ride> {
        val passengerDoc = firestore.collection("users")
            .document(passengerId)
            .collection("passengerData")
            .document("info")
            .get()
            .await()

        val passenger = passengerDoc.toObject(Passenger::class.java) ?: return emptyList()
        val activeIds = passenger.activeRideId

        if (activeIds.isEmpty()) return emptyList()

        val rides = mutableListOf<Ride>()
        for (rideId in activeIds) {
            val rideSnapshot = firestore.collection("rides").document(rideId).get().await()
            val ride = rideSnapshot.toObject(Ride::class.java)
            if (ride != null) rides.add(ride)
        }
        return rides
    }
}