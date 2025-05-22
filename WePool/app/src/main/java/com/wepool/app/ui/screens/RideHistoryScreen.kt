package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun RideHistoryScreen(navController: NavController, uid: String) {
    val rideRepository: IRideRepository = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf<UserRole?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }

    var hasFilterBeenApplied by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        null to "All",
        UserRole.DRIVER to "Driver",
        UserRole.PASSENGER to "Passenger",
    )

    fun refreshRides() {
        coroutineScope.launch {
            try {
                loading = true
                error = null
                rides = when (selectedFilter) {
                    UserRole.DRIVER -> rideRepository.getPastRidesAsDriver(uid)
                    UserRole.PASSENGER -> rideRepository.getPastRidesAsPassenger(uid)
                    null -> {
                        val driverRides = rideRepository.getPastRidesAsDriver(uid)
                        val passengerRides = rideRepository.getPastRidesAsPassenger(uid)
                        (driverRides + passengerRides).distinctBy { it.rideId }
                    }
                    else -> emptyList()
                }
                hasFilterBeenApplied = true
            } catch (e: Exception) {
                error = "❌ שגיאה בטעינת נסיעות: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 64.dp) // רווח תחתון כדי לא להסתיר ע"י הכפתור
        ) {
            Text(
                text = "Ride History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Filter by role",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = filterOptions.first { it.first == selectedFilter }.second,
                                    modifier = Modifier.align(Alignment.Center),
                                    fontSize = 18.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            filterOptions.forEach { (filter, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label)
                                        }
                                    },
                                    onClick = {
                                        selectedFilter = filter
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { refreshRides() },
                        modifier = Modifier.fillMaxWidth(0.75f).height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Refresh")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                !hasFilterBeenApplied -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Please select a filter and press Refresh to show rides.",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                rides.isEmpty() -> Text(
                    "No ride history found for selected role.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                    ) {
                        items(rides) { ride ->
                            RideHistoryCard(ride = ride)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid?fromLogin=false") {
                    popUpTo("intermediate/$uid?fromLogin=false") { inclusive = false }
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Home", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun RideHistoryCard(ride: Ride) {
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()

    var selectedPassenger by remember { mutableStateOf<User?>(null) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var passengerMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var driverUser by remember { mutableStateOf<User?>(null) }
    var isDriverDialogOpen by remember { mutableStateOf(false) }
    var selectedDriverUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(ride.driverId) {
        ride.driverId.let { driverId ->
            try {
                driverUser = userRepository.getUser(driverId)
            } catch (e: Exception) {
                Log.e("RideCard", "❌ שגיאה בטעינת נהג $driverId: ${e.message}")
            }
        }
    }

    LaunchedEffect(ride.passengers) {
        val loaded = mutableMapOf<String, User>()
        ride.passengers.forEach { uid ->
            try {
                val user = userRepository.getUser(uid)
                loaded[uid] = user!!
            } catch (e: Exception) {
                Log.e("RideCard", "❌ שגיאה בטעינת נוסע $uid: ${e.message}")
            }
        }
        passengerMap = loaded
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Date: ${ride.date}")
            Text("From: ${ride.startLocation.name}")
            Text("To: ${ride.destination.name}")
            Text("Departure Time: ${ride.departureTime}")
            Text("Arrival Time: ${ride.arrivalTime}")

            Spacer(modifier = Modifier.height(8.dp))

            driverUser?.let { driver ->
                Text("Driver:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = driver.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            selectedDriverUser = driver
                            isDriverDialogOpen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Show driver details"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("Passenger List:", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))

            passengerMap.values.forEach { passenger ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = passenger.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            selectedPassenger = passenger
                            isDialogOpen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Show details for ${passenger.name}"
                        )
                    }
                }
            }
        }
    }

    if (isDialogOpen && selectedPassenger != null) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = selectedPassenger!!.name)
                }
            },
            text = {
                Column {
                    Text("Email: ${selectedPassenger!!.email}")
                    Text("Phone: ${selectedPassenger!!.phoneNumber}")

                    Spacer(modifier = Modifier.height(8.dp))

                    val pickupStop = ride.pickupStops.firstOrNull { it.passengerId == selectedPassenger!!.uid }
                    var locationLabel = ""
                    var timeLabel = ""
                    var timeReference = ""
                    if (ride.direction == RideDirection.TO_WORK) {
                        locationLabel = "Pickup Location"
                        timeLabel = "Pickup Time"
                        timeReference = pickupStop?.pickupTime ?: "Unknown"
                    } else {
                        locationLabel = "Dropoff Location"
                        timeLabel = "Departure Time"
                        timeReference = pickupStop?.dropoffTime ?: "Unknown"
                    }
                    val locationName = pickupStop?.location?.name ?: "Unknown"

                    Text("$locationLabel: $locationName")
                    Text("$timeLabel: $timeReference")
                }
            },
            confirmButton = {
                Button(
                    onClick = { isDialogOpen = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (isDriverDialogOpen && selectedDriverUser != null) {
        AlertDialog(
            onDismissRequest = { isDriverDialogOpen = false },
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = selectedDriverUser!!.name)
                }
            },
            text = {
                Column {
                    Text("Email: ${selectedDriverUser!!.email}")
                    Text("Phone: ${selectedDriverUser!!.phoneNumber}")
                }
            },
            confirmButton = {
                Button(
                    onClick = { isDriverDialogOpen = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }
}



