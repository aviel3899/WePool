package com.wepool.app

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wepool.app.ui.theme.WePoolTheme
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.RideNavigationServiceController
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
import com.wepool.app.ui.screens.PassengerActiveRidesScreen
import com.wepool.app.ui.screens.PassengerMenuScreen
import com.wepool.app.ui.screens.DriverActiveRidesScreen
import com.wepool.app.ui.screens.DriverPendingRequestsScreen
import com.wepool.app.ui.screens.PassengerPendingRequestsScreen

class MainActivity : ComponentActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI
        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

                LaunchedEffect(Unit) {
                    if (!locationPermissionState.status.isGranted) {
                        locationPermissionState.launchPermissionRequest()
                    }

                    requestUsageStatsPermissionIfNeeded(context)

                    RepositoryProvider.provideRideRepository().deactivateExpiredRides()

                    val driverId = "nC8UiZonOSRmf4DnxiaikCXypgt2"
                    val activeRides = RepositoryProvider.provideDriverRepository().getActiveRidesForDriver(driverId)
                    val activeRide = activeRides.firstOrNull()

                    if (activeRide != null) {
                        RideNavigationServiceController.startRideNavigation(context, activeRide.rideId)
                    }
                }

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
                        composable("intermediate/{uid}?fromLogin={fromLogin}") { backStackEntry ->
                            val uid =
                                backStackEntry.arguments?.getString("uid") ?: return@composable
                            val fromLogin = backStackEntry.arguments?.getString("fromLogin")?.toBooleanStrictOrNull() ?: false
                            IntermediateScreen(navController = navController, uid = uid, cameFromLogin = fromLogin)
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
                        composable("passengerMenu/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            PassengerMenuScreen(uid = uid, navController = navController)
                        }
                        composable("passengerActiveRides/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            PassengerActiveRidesScreen(uid = uid, navController = navController)
                        }
                        composable("driverActiveRides/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            DriverActiveRidesScreen(uid = uid, navController = navController)
                        }
                        composable("driverPendingRequests/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            DriverPendingRequestsScreen(uid = uid, navController = navController)
                        }
                        composable("passengerPendingRequests/{uid}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                            PassengerPendingRequestsScreen(uid = uid, navController = navController)
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestUsageStatsPermissionIfNeeded(context: Context) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

}