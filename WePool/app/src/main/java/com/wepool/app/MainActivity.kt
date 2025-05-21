package com.wepool.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import com.wepool.app.data.repository.LoginSessionManager
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.notifications.NotificationHelper
import com.wepool.app.ui.screens.*
import com.wepool.app.ui.theme.WePoolTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    init {
        RepositoryProvider.initialize(BuildConfig.MAPS_API_KEY)
    }

    private val mainScope = MainScope()
    private var navController: NavHostController? = null
    private lateinit var context: Context

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            WePoolTheme {
                navController = rememberNavController()
                val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
                } else null

                context = LocalContext.current

                val screenState = remember { mutableStateOf<String?>(null) }
                val rideIdState = remember { mutableStateOf<String?>(null) }
                val fromNotificationState = remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val stored = NotificationHelper.getStoredNotificationData(context)
                    screenState.value = intent?.getStringExtra("screen") ?: stored.first
                    rideIdState.value = intent?.getStringExtra("rideId") ?: stored.second
                    fromNotificationState.value = intent?.getStringExtra("fromNotification") == "true"

                    Log.d("MainActivity", "📦 rideId from intent: ${rideIdState.value}")
                    Log.d("MainActivity", "📥 Intent received: rideId=${rideIdState.value}, screen=${screenState.value}, fromNotification=${fromNotificationState.value}")

                    if (!fromNotificationState.value) {
                        LoginSessionManager.clearLoginFlag(context)
                    }

                    RepositoryProvider.provideRideRepository().deactivateExpiredRides()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        notificationPermissionState?.status?.isGranted == false
                    ) {
                        notificationPermissionState.launchPermissionRequest()
                    }

                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancelAll()

                    handleNavigation(screenState.value, rideIdState.value, fromNotificationState.value)
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
                            val fromLogin = it.arguments?.getString("fromLogin")?.toBooleanStrictOrNull() ?: false
                            IntermediateScreen(navController!!, uid, fromLogin)
                        }
                        composable("rideHistoryMenu/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            RideHistoryMenuScreen(navController!!, uid)
                        }
                        composable("updateDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            UpdateDetailsScreen(navController!!, uid)
                        }
                        composable("createRideDirection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            CreateRideDirectionScreen(navController!!, uid)
                        }
                        composable("homeboundRide/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            HomeboundRideCreationScreen(navController!!, uid)
                        }
                        composable("workboundRide/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            WorkboundRideCreationScreen(navController!!, uid)
                        }
                        composable("driverCarDetails/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            DriverCarDetailsScreen(uid, navController!!)
                        }
                        composable("passengerRideDirection/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerRideDirectionScreen(navController!!, uid)
                        }
                        composable("passengerHomeboundRideSearch/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerHomeboundRideSearchScreen(navController!!, uid)
                        }
                        composable("passengerWorkboundRideSearch/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerWorkboundRideSearchScreen(navController!!, uid)
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
                            PassengerActiveRidesScreen(uid = uid, navController = navController!!, rideId = rideId)
                        }
                        composable("driverActiveRides/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            DriverActiveRidesScreen(uid = uid, navController = navController!!, rideId = rideId)
                        }
                        composable("driverPendingRequests/{uid}?rideId={rideId}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            val rideId = it.arguments?.getString("rideId")
                            DriverRequestsScreen(uid = uid, navController = navController!!, filterRideId = rideId)
                        }
                        composable("passengerPendingRequests/{uid}") {
                            val uid = it.arguments?.getString("uid") ?: return@composable
                            PassengerRequestsScreen(uid, navController!!)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent){
        super.onNewIntent(intent)
        setIntent(intent)
        val screen = intent.getStringExtra("screen")
        val rideId = intent.getStringExtra("rideId")
        val fromNotification = intent.getStringExtra("fromNotification") == "true"
        handleNavigation(screen, rideId, fromNotification)
    }

    private fun handleNavigation(screen: String?, rideId: String?, fromNotification: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val didLoginManually = LoginSessionManager.didLoginManually(context)

        if (currentUser != null && !didLoginManually) {
            navController?.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
            return
        }

        if (fromNotification && currentUser != null && didLoginManually && !screen.isNullOrEmpty() && !rideId.isNullOrEmpty()) {
            mainScope.launch {
                val uid = currentUser.uid
                val user = RepositoryProvider.provideUserRepository().getUser(uid)
                val ride = RepositoryProvider.provideRideRepository().getRide(rideId)
                val isDriver = ride?.driverId == uid
                val isPassenger = ride?.passengers?.contains(uid) == true

                val route = when (screen) {
                    "rideStarted", "pickup", "dropoff", "rideUpdated", "rideCancelled" ->
                        when {
                            isPassenger -> "passengerActiveRides/$uid?rideId=$rideId"
                            isDriver -> "driverActiveRides/$uid?rideId=$rideId"
                            user?.roles?.contains("DRIVER") == true -> "driverMenu/$uid"
                            else -> "passengerMenu/$uid"
                        }

                    "pendingRequests" ->
                        if (user?.roles?.contains("DRIVER") == true)
                            "driverPendingRequests/$uid?rideId=$rideId"
                        else
                            "passengerPendingRequests/$uid"

                    else ->
                        if (user?.roles?.contains("DRIVER") == true)
                            "driverMenu/$uid"
                        else
                            "passengerMenu/$uid"
                }

                navController?.navigate(route) {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }
}
