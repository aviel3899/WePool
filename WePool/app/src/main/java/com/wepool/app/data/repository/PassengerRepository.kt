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
}
