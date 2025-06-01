package com.wepool.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.repository.LoginSessionManager
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.handleNotificationNavigation
import com.wepool.app.notifications.NotificationHelper
import com.wepool.app.ui.screens.adminScreens.AdminMenuScreen
import com.wepool.app.ui.screens.adminScreens.companies.CompanyListScreen
import com.wepool.app.ui.screens.adminScreens.users.UserListScreen
import com.wepool.app.ui.screens.adminScreens.rides.RidesListScreen
import com.wepool.app.ui.screens.driverScreens.DriverActiveRidesScreen
import com.wepool.app.ui.screens.driverScreens.DriverCarDetailsScreen
import com.wepool.app.ui.screens.driverScreens.DriverMenuScreen
import com.wepool.app.ui.screens.driverScreens.DriverRequestsScreen
import com.wepool.app.ui.screens.driverScreens.RideCreationScreen
import com.wepool.app.ui.screens.mainScreens.IntermediateScreen
import com.wepool.app.ui.screens.mainScreens.LoginScreen
import com.wepool.app.ui.screens.mainScreens.RoleSelectionScreen
import com.wepool.app.ui.screens.mainScreens.SignUpScreen
import com.wepool.app.ui.screens.mainScreens.RideDirectionScreen
import com.wepool.app.ui.screens.mainScreens.UpdateDetailsScreen
import com.wepool.app.ui.screens.favoriteLocations.PreferredLocationsScreen
import com.wepool.app.ui.screens.rideHistory.RideHistoryScreen
import com.wepool.app.ui.screens.passengerScreens.PassengerActiveRidesScreen
import com.wepool.app.ui.screens.passengerScreens.PassengerMenuScreen
import com.wepool.app.ui.screens.passengerScreens.PassengerRequestsScreen
import com.wepool.app.ui.screens.passengerScreens.PassengerRideSearchScreen
import com.wepool.app.ui.screens.hrManagerScreens.HRManagerMenuScreen
import com.wepool.app.ui.screens.hrManagerScreens.HRManageEmployeesScreen
import com.wepool.app.ui.screens.hrManagerScreens.HRManageCompanyScreen
import com.wepool.app.ui.screens.hrManagerScreens.HRManagerRidesScreen
import com.wepool.app.ui.theme.WePoolTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    private var navController: NavHostController? = null
    private var wasNavigatedFromIntent = mutableStateOf(false)

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screen = intent?.getStringExtra("screen")
        val rideId = intent?.getStringExtra("rideId")
        val fromNotification = intent?.getStringExtra("fromNotification") == "true"

        Log.d(
            "MainActivity",
            "📥 onNewIntent: rideId=$rideId, screen=$screen, fromNotification=$fromNotification"
        )

        if (fromNotification && !screen.isNullOrEmpty() && !rideId.isNullOrEmpty()) {
            NotificationHelper.storeNotificationData(this, screen, rideId)
        }

        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                navController = rememberNavController()
                val context = LocalContext.current
                val notificationPermissionState =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else null

                val isLoggedIn = LoginSessionManager.didLoginManually(context)
                val firebaseUser = FirebaseAuth.getInstance().currentUser

                LaunchedEffect(firebaseUser, isLoggedIn) {
                    if (firebaseUser != null && isLoggedIn && !wasNavigatedFromIntent.value) {
                        val uid = firebaseUser.uid
                        navController?.navigate("roleSelection/$uid") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                        wasNavigatedFromIntent.value = true
                    }
                }

                LaunchedEffect(Unit) {
                    RepositoryProvider.provideRideRepository().deactivateExpiredRides()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        notificationPermissionState?.status?.isGranted == false
                    ) {
                        notificationPermissionState.launchPermissionRequest()
                    }

                    val manager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancelAll()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController!!,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") { LoginScreen(navController!!) }
                        composable("signup") { SignUpScreen(navController!!) }
                        composable("roleSelection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            RoleSelectionScreen(uid, navController!!)
                        }
                        composable("intermediate/{uid}?fromLogin={fromLogin}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            IntermediateScreen(navController!!, uid)
                        }
                        composable("rideHistory/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            RideHistoryScreen(navController!!, uid)
                        }
                        composable("updateDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            UpdateDetailsScreen(navController!!, uid)
                        }
                        composable("driverCarDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverCarDetailsScreen(uid, navController!!)
                        }
                        composable("driverMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverMenuScreen(navController!!, uid)
                        }
                        composable("passengerMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerMenuScreen(uid, navController!!)
                        }
                        composable("passengerActiveRides/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            PassengerActiveRidesScreen(
                                uid = uid,
                                navController = navController!!,
                                rideId = rideId
                            )
                        }
                        composable("driverActiveRides/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            DriverActiveRidesScreen(
                                uid = uid,
                                navController = navController!!,
                                rideId = rideId
                            )
                        }
                        composable("driverPendingRequests/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            DriverRequestsScreen(
                                uid = uid,
                                navController = navController!!,
                                filterRideId = rideId
                            )
                        }
                        composable("passengerPendingRequests/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            PassengerRequestsScreen(
                                uid = uid,
                                navController = navController!!,
                                filterRideId = rideId
                            )
                        }
                        composable("preferredLocations/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PreferredLocationsScreen(uid, navController!!)
                        }
                        composable("passengerRideSearch/{uid}/{direction}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val directionStr =
                                it.arguments?.getString("direction") ?: return@composable
                            val direction = RideDirection.valueOf(directionStr)
                            PassengerRideSearchScreen(navController!!, uid, direction)
                        }
                        composable("rideCreation/{uid}/{direction}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val directionStr =
                                it.arguments?.getString("direction") ?: return@composable
                            val direction = RideDirection.valueOf(directionStr)
                            RideCreationScreen(
                                navController = navController!!,
                                uid = uid,
                                direction = direction
                            )
                        }
                        appNavGraph(navController!!) // ride direction selection screen
                        composable("hrManagerMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            HRManagerMenuScreen(uid = uid, navController = navController!!)
                        }
                        composable("adminMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            AdminMenuScreen(uid = uid, navController = navController!!)
                        }
                        composable("companyList/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            CompanyListScreen(uid = uid, navController = navController!!)
                        }
                        composable("hrManageEmployees/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            HRManageEmployeesScreen(uid = uid, navController = navController!!)
                        }
                        composable("hrManageCompany/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            HRManageCompanyScreen(uid = uid, navController = navController!!)
                        }
                        composable("hrManagerRides/{uid}?filter={filter}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid").orEmpty()
                            val filter = backStackEntry.arguments?.getString("filter") == "true"
                            HRManagerRidesScreen(uid = uid, navController = navController!!, filterByUid = filter)
                        }
                        composable("userList/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            UserListScreen(uid = uid, navController = navController!!)
                        }
                        composable("ridesList/{uid}?filter={filter}") { backStackEntry ->
                            val uid = backStackEntry.arguments?.getString("uid").orEmpty()
                            val filter = backStackEntry.arguments?.getString("filter") == "true"
                            RidesListScreen(uid = uid, navController = navController!!, filterByUid = filter)
                        }
                    }
                }
            }
        }
    }

    private fun NavGraphBuilder.appNavGraph(navController: NavController) {
        composable("rideDirection/{uid}/{role}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            val role = backStackEntry.arguments?.getString("role") ?: return@composable

            RideDirectionScreen(
                navController = navController,
                uid = uid,
                onToWorkClick = {
                    if (role == "DRIVER") {
                        navController.navigate("rideCreation/$uid/TO_WORK")
                    } else {
                        navController.navigate("passengerRideSearch/$uid/TO_WORK")
                    }
                },
                onToHomeClick = {
                    if (role == "DRIVER") {
                        navController.navigate("rideCreation/$uid/TO_HOME")
                    } else {
                        navController.navigate("passengerRideSearch/$uid/TO_HOME")
                    }
                }
            )
        }
    }

    override fun onStop() {
        Log.d("MainActivity", "\uD83E\uDEB9 האפליקציה נסגרת – איפוס דגל התחברות")
        val isFinishingApp = isFinishing || !isChangingConfigurations
        if (isFinishingApp) {
            LoginSessionManager.clearLoginFlag(this)
        }
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val screen = intent.getStringExtra("screen")
        val rideId = intent.getStringExtra("rideId")
        val fromNotification = intent.getBooleanExtra("fromNotification", false) == true

        Log.d(
            "MainActivity",
            "📥 onNewIntent: rideId=$rideId, screen=$screen, fromNotification=$fromNotification"
        )

        if (fromNotification && !screen.isNullOrEmpty() && !rideId.isNullOrEmpty()) {
            NotificationHelper.storeNotificationData(this, screen, rideId)

            val currentUser = FirebaseAuth.getInstance().currentUser
            val didLoginManually = LoginSessionManager.didLoginManually(this)

            if (currentUser != null && didLoginManually) {
                lifecycleScope.launch {
                    handleNotificationNavigation(this@MainActivity, navController!!)
                    wasNavigatedFromIntent.value = true
                }
            }
        }
    }
}