package com.wepool.app.infrastructure

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wepool.app.data.remote.GoogleMapsService
import com.wepool.app.data.repository.AdminRepository
import com.wepool.app.data.repository.DriverRepository
import com.wepool.app.data.repository.PassengerRepository
import com.wepool.app.data.repository.UserRepository
import com.wepool.app.data.repository.AuthRepository
import com.wepool.app.data.repository.LocationDataRepository
import com.wepool.app.data.repository.RideRepository
import com.wepool.app.data.repository.RideRequestRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import com.wepool.app.data.repository.interfaces.ILocationDataRepository
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import com.wepool.app.data.repository.interfaces.ICompanyRepository
import com.wepool.app.data.repository.CompanyRepository
import com.wepool.app.data.repository.HRManagerRepository
import com.wepool.app.data.repository.interfaces.IAdminRepository
import com.wepool.app.data.repository.interfaces.IHRManagerRepository

object RepositoryProvider {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private lateinit var apiKey: String

    fun initialize(apiKey: String) {
        this.apiKey = apiKey
    }

    val mapsService: GoogleMapsService by lazy {
        GoogleMapsService(apiKey)
    }

    fun provideAuthRepository(): AuthRepository {
        return AuthRepository(auth = auth, userRepository = provideUserRepository())
    }

    fun provideLocationDataRepository(): ILocationDataRepository {
        return LocationDataRepository(firestore = firestore)
    }

    fun provideUserRepository(): IUserRepository {
        return UserRepository(auth = auth, db = firestore)
    }

    fun provideDriverRepository(): IDriverRepository {
        return DriverRepository(auth = auth, firestore = firestore)
    }

    fun providePassengerRepository(): IPassengerRepository {
        return PassengerRepository(firestore = firestore)
    }

    fun provideHRManagerRepository(): IHRManagerRepository {
        return HRManagerRepository(firestore = firestore, userRepository = provideUserRepository(), companyRepository = provideCompanyRepository())
    }

    fun provideAdminRepository(): IAdminRepository {
        return AdminRepository(
            userRepository = provideUserRepository(),
            hrManagerRepository = provideHRManagerRepository(),
            companyRepository = provideCompanyRepository()
        )
    }

    fun provideCompanyRepository(): ICompanyRepository {
        return CompanyRepository(
            firestore = firestore, mapsService = mapsService, userRepository = provideUserRepository())
    }

    fun provideRideRepository(): IRideRepository {
        return RideRepository(firestore = firestore, mapsService = mapsService, rideRequestRepository = provideRideRequestRepository())
    }

    fun provideRideRequestRepository(): IRideRequestRepository {
        return RideRequestRepository(firestore = firestore)
    }

    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

}