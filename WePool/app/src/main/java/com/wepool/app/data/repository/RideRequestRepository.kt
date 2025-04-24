package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.toObject
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RideRequestRepository(
    private val firestore: FirebaseFirestore
) : IRideRequestRepository {

    override suspend fun sendRequest(
        rideId: String,
        passengerId: String,
        pickupLocation: GeoPoint
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestId = firestore.collection("rides").document(rideId)
                .collection("requests").document().id

            val request = RideRequest(
                requestId = requestId,
                rideId = rideId,
                passengerId = passengerId,
                pickupLocation = pickupLocation,
                status = RequestStatus.PENDING
            )

            firestore.collection("rides").document(rideId)
                .collection("requests").document(requestId)
                .set(request)
                .await()

            Log.d("RideRequest", "✅ בקשה נשלחה (requestId: $requestId)")
            return@withContext true

        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בשליחת בקשה: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun updateRequestStatus(rideId: String, requestId: String, newStatus: RequestStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestRef = firestore.collection("rides")
                .document(rideId)
                .collection("requests")
                .document(requestId)

            requestRef.update("status", newStatus).await()

            Log.d("RideRequest", "🔄 סטטוס עודכן ל-$newStatus (requestId: $requestId)")
            return@withContext true
        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בעדכון סטטוס: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun getRequestsForRide(rideId: String): List<RideRequest> = withContext(Dispatchers.IO) {
        return@withContext try {
            firestore.collection("rides").document(rideId)
                .collection("requests")
                .get().await()
                .toObjects(RideRequest::class.java)
        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בשליפת בקשות לפי rideId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getRequestsByPassenger(passengerId: String): List<RideRequest> = withContext(Dispatchers.IO) {
        return@withContext try {
            val allRides = firestore.collection("rides").get().await()
            val result = mutableListOf<RideRequest>()

            for (rideDoc in allRides.documents) {
                val rideId = rideDoc.id
                val requests = firestore.collection("rides").document(rideId)
                    .collection("requests")
                    .whereEqualTo("passengerId", passengerId)
                    .get().await()
                    .toObjects(RideRequest::class.java)

                result += requests
            }

            result
        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בשליפת בקשות לפי passengerId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getRequestsByDriver(driverId: String): List<RideRequest> = withContext(Dispatchers.IO) {
        return@withContext try {
            val allRides = firestore.collection("rides")
                .whereEqualTo("driverId", driverId)
                .get()
                .await()

            val result = mutableListOf<RideRequest>()

            for (rideDoc in allRides.documents) {
                val rideId = rideDoc.id
                val requests = firestore.collection("rides").document(rideId)
                    .collection("requests")
                    .get()
                    .await()
                    .toObjects(RideRequest::class.java)

                result += requests
            }

            result
        } catch (e: Exception) {
            Log.e("RideRequest", "❌ שגיאה בשליפת בקשות לפי driverId: ${e.message}", e)
            emptyList()
        }
    }


    override suspend fun deleteRequest(rideId: String, requestId: String) {
            firestore.collection("rides")
                .document(rideId)
                .collection("requests")
                .document(requestId)
                .delete()
                .await()
    }

}
