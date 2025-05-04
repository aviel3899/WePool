package com.wepool.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.wepool.app.ui.theme.WePoolTheme
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.users.Driver
import com.wepool.app.data.model.users.Passenger
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import com.wepool.app.data.repository.interfaces.IDriverRepository
import com.wepool.app.data.repository.interfaces.IPassengerRepository
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import com.wepool.app.data.model.logic.PassengerRideFinder
import com.wepool.app.infrastructure.RepositoryProvider
import com.google.android.gms.maps.model.LatLng
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wepool.app.ui.screens.LoginScreen
import com.wepool.app.ui.screens.SignUpScreen
import com.wepool.app.ui.screens.RoleSelectionScreen
import com.wepool.app.ui.screens.IntermediateScreen
import com.wepool.app.ui.screens.RideHistoryMenuScreen
import com.wepool.app.ui.screens.UpdateDetailsScreen
import com.wepool.app.ui.screens.CreateRideDirectionScreen
import com.wepool.app.ui.screens.DriverCarDetailsScreen
import com.wepool.app.ui.screens.HomeboundRideCreationScreen
import com.wepool.app.ui.screens.PassengerHomeboundRideSearchScreen
import com.wepool.app.ui.screens.PassengerRideDirectionScreen
import com.wepool.app.ui.screens.PassengerWorkboundRideSearchScreen
import com.wepool.app.ui.screens.WorkboundRideCreationScreen
import com.wepool.app.ui.screens.DriverMenuScreen

class MainActivity : ComponentActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    private val authRepository = RepositoryProvider.provideAuthRepository()
    private val userRepository: IUserRepository = RepositoryProvider.provideUserRepository()
    private val driverRepository: IDriverRepository = RepositoryProvider.provideDriverRepository()
    private val passengerRepository: IPassengerRepository =
        RepositoryProvider.providePassengerRepository()
    private val rideRepository: IRideRepository = RepositoryProvider.provideRideRepository()
    private val rideRequestRepository: IRideRequestRepository =
        RepositoryProvider.provideRideRequestRepository()
    private val mapsService = RepositoryProvider.mapsService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val email = "test@wepool.com"
        val password = "test1234"

        // UI
        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(navController = navController)
                        }
                        composable("signup") {
                            SignUpScreen(navController = navController)
                        }
                        composable("roleSelection/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            RoleSelectionScreen(navController = navController, uid = uid)
                        }
                        composable("intermediate/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            IntermediateScreen(navController = navController, uid = uid)
                        }
                        composable("rideHistoryMenu/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            RideHistoryMenuScreen(navController = navController, uid = uid)
                        }

                        composable("rideHistoryDriver/{uid}") {
                            Text("📘 Ride History as a Driver (Placeholder)")
                        }

                        composable("rideHistoryPassenger/{uid}") {
                            Text("📕 Ride History as a Passenger (Placeholder)")
                        }

                        composable("rideHistoryCombined/{uid}") {
                            Text("📗 Ride History Combined (Placeholder)")
                        }
                        composable("updateDetails/{uid}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            UpdateDetailsScreen(navController = navController, uid = uid)
                        }
                        composable("createRideDirection/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            CreateRideDirectionScreen(navController = navController, uid = uid)
                        }
                        composable("homeboundRide/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            HomeboundRideCreationScreen(navController = navController, uid = uid)
                        }
                        composable("workboundRide/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            WorkboundRideCreationScreen(navController = navController, uid = uid)
                        }
                        composable("driverCarDetails/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            DriverCarDetailsScreen(uid = uid, navController = navController)
                        }
                        composable("passengerRideDirection/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            PassengerRideDirectionScreen(navController = navController, uid = uid)
                        }
                        composable("passengerHomeboundRideSearch/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            PassengerHomeboundRideSearchScreen(navController = navController, uid = uid)
                        }

                        composable("passengerWorkboundRideSearch/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            PassengerWorkboundRideSearchScreen(navController = navController, uid = uid)
                        }

                        composable("driverMenu/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            DriverMenuScreen(navController = navController, uid = uid)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        WePoolTheme {
            Greeting("Android")
        }
    }
}