package com.wepool.app.infrastructure

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.remote.GoogleMapsService
import com.wepool.app.data.repository.DriverRepository
import com.wepool.app.data.repository.UserRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.repository.interfaces.IUserRepository

object RepositoryProvider {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun provideUserRepository(): IUserRepository {
        return UserRepository(auth = auth, db = firestore)
    }

    fun provideDriverRepository(apiKey: String): IDriverRepository {
        val mapsService = GoogleMapsService(apiKey)
        return DriverRepository(auth = auth, firestore = firestore, mapsService = mapsService)
    }

    //need to create PassengerRepository

    //need to create HRManagerRepository

    //need to create AdminRepository
}
