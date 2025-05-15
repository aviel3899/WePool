package com.wepool.app

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.messaging.FirebaseMessaging
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.*
import com.wepool.app.ui.theme.WePoolTheme

class MainActivity : ComponentActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                val navController = rememberNavController()
                val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    null
                }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    RepositoryProvider.provideRideRepository().deactivateExpiredRides()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !notificationPermissionState!!.status.isGranted
                    ) {
                        notificationPermissionState.launchPermissionRequest()
                    }

                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancelAll()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") { LoginScreen(navController) }
                        composable("signup") { SignUpScreen(navController) }
                        composable("roleSelection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            RoleSelectionScreen(uid, navController)
                        }
                        composable("intermediate/{uid}?fromLogin={fromLogin}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val fromLogin = it.arguments?.getString("fromLogin")?.toBooleanStrictOrNull() ?: false
                            IntermediateScreen(navController, uid, fromLogin)
                        }
                        composable("rideHistoryMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            RideHistoryMenuScreen(navController, uid)
                        }
                        composable("rideHistoryDriver/{uid}") { /* ... */ }
                        composable("rideHistoryPassenger/{uid}") { /* ... */ }
                        composable("rideHistoryCombined/{uid}") { /* ... */ }
                        composable("updateDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            UpdateDetailsScreen(navController, uid)
                        }
                        composable("createRideDirection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            CreateRideDirectionScreen(navController, uid)
                        }
                        composable("homeboundRide/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            HomeboundRideCreationScreen(navController, uid)
                        }
                        composable("workboundRide/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            WorkboundRideCreationScreen(navController, uid)
                        }
                        composable("driverCarDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverCarDetailsScreen(uid, navController)
                        }
                        composable("passengerRideDirection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerRideDirectionScreen(navController, uid)
                        }
                        composable("passengerHomeboundRideSearch/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerHomeboundRideSearchScreen(navController, uid)
                        }
                        composable("passengerWorkboundRideSearch/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerWorkboundRideSearchScreen(navController, uid)
                        }
                        composable("driverMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverMenuScreen(navController, uid)
                        }
                        composable("passengerMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerMenuScreen(uid, navController)
                        }
                        composable("passengerActiveRides/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerActiveRidesScreen(uid, navController)
                        }
                        composable("driverActiveRides/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverActiveRidesScreen(uid, navController)
                        }
                        composable("driverPendingRequests/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverRequestsScreen(uid, navController)
                        }
                        composable("passengerPendingRequests/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerRequestsScreen(uid, navController)
                        }
                    }
                }
            }
        }
    }
}
