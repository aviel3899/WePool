package com.wepool.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import kotlinx.coroutines.tasks.await

class PassengerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IPassengerRepository {

    private val TAG = "PassengerRepository"

    // פונקציית לוג אחידה להודעות מערכת
    private fun log(tag: String, message: String, error: Exception? = null) {
        if (error != null) {
            Log.e(tag, "❌ $message", error)
        } else {
            Log.d(tag, "✅ $message")
        }
    }

    // יצירה או עדכון של נוסע
    override suspend fun createOrUpdatePassenger(uid: String, passenger: Passenger) {
        try {
            db.collection("users")
                .document(uid)
                .collection("passengerData")
                .document("main")
                .set(passenger)
                .await()
            log(TAG, "Passenger created/updated successfully")
        } catch (e: Exception) {
            log(TAG, "Failed to create/update passenger", e)
        }
    }

    // שליפה של נתוני נוסע לפי UID
    override suspend fun getPassenger(uid: String): Passenger? {
        return try {
            val doc = db.collection("users")
                .document(uid)
                .collection("passengerData")
                .document("main")
                .get()
                .await()

            if (doc.exists()) {
                log(TAG, "Passenger fetched successfully")
                doc.toObject<Passenger>()
            } else {
                log(TAG, "Passenger document not found")
                null
            }
        } catch (e: Exception) {
            log(TAG, "Failed to fetch passenger", e)
            null
        }
    }
}
