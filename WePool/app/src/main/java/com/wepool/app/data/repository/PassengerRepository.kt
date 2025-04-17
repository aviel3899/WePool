package com.wepool.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PassengerRepository(
    private val firestore: FirebaseFirestore
) : IPassengerRepository {

     // מחזיר נוסע לפי uid
     override suspend fun getPassenger(uid: String): Passenger? {
         val snapshot = firestore.collection("users")
             .document(uid)
             .collection("passengerData")
             .document("info")
             .get()
             .await()

         return snapshot.toObject(Passenger::class.java)
     }

      // מעדכן את מיקום האיסוף המועדף של הנוסע במסמך
    override suspend fun updatePreferredPickupLocation(uid: String, location: GeoPoint)  {
        firestore.collection("users")
            .document(uid)
            .collection("passengerData")
            .document("info")
            .update("preferredPickupLocation", location)
            .await()
    }

    // מעדכן את זמן ההגעה של הנוסע
    override suspend fun updatePreferredArrivalTime(uid: String, arrivalTime: String)  {
        firestore.collection("users")
            .document(uid)
            .collection("passengerData")
            .document("info")
            .update("preferredArrivalTime", arrivalTime)
            .await()
    }

    // שומר את נתוני הנוסע - מעדכן אם כבר קיימים נתונים, יוצר חדש אם לא קיימים
    override suspend fun savePassengerData(uid: String, passenger: Passenger)  {
        firestore.collection("users")
            .document(uid)
            .collection("passengerData")
            .document("info")
            .set(passenger)
            .await()
    }
}

