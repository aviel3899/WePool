package com.wepool.app.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.RideNavigationServiceController
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DriverActiveRidesScreen(uid: String, navController: NavController, rideId: String? = null) {
    val context = LocalContext.current
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val requestRepository = RepositoryProvider.provideRideRequestRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isActionInProgress by remember { mutableStateOf(false) }
    var selectedRide by remember { mutableStateOf<Ride?>(null) }
    var passengerNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedPassengerStop by remember { mutableStateOf<PickupStop?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showTooLateDialog by remember { mutableStateOf(false) }
    var innerTooLateDialog by remember { mutableStateOf(false) }
    var threshold by remember { mutableStateOf(0) }

    fun refreshRides() {
        coroutineScope.launch {
            try {
                loading = true
                error = null
                rides = driverRepository.getActiveRidesForDriver(uid)
            } catch (e: Exception) {
                error = "❌ שגיאה בטעינת נסיעות: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun calculateTimeDifferenceInMinutes(start: String, end: String): Int {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startTime = LocalTime.parse(start, formatter)
        val endTime = LocalTime.parse(end, formatter)
        return abs(java.time.Duration.between(startTime, endTime).toMinutes().toInt())
    }

    LaunchedEffect(Unit) {
        refreshRides()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your Active Rides", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            rides.isEmpty() -> Text("You have no active rides at the moment.")
            else -> {
                val filteredRides = if (!rideId.isNullOrEmpty()) rides.filter { it.rideId == rideId } else rides

                LazyColumn {
                    items(filteredRides) { ride ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("From: ${ride.startLocation.name}")
                                Text("To: ${ride.destination.name}")
                                Text("Date: ${ride.date}")
                                Text("Departure Time: ${ride.departureTime}")
                                Text("Arrival Time: ${ride.arrivalTime}")
                                Text("Stops on the way: ${ride.pickupStops.size}")
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(onClick = {
                                        coroutineScope.launch {
                                            if (isActionInProgress) return@launch
                                            isActionInProgress = true
                                            if (!locationPermissionState.status.isGranted) {
                                                locationPermissionState.launchPermissionRequest()
                                                error = "📍 נדרש לאשר גישה למיקום"
                                                isActionInProgress = false
                                                return@launch
                                            }
                                            try {
                                                RideNavigationServiceController.startRideNavigation(context, ride.rideId)
                                            } catch (e: Exception) {
                                                error = "⚠️ ${e.message}"
                                            } finally {
                                                isActionInProgress = false
                                            }
                                        }
                                    }) {
                                        Text("Start Ride")
                                    }

                                    OutlinedButton(onClick = {
                                        coroutineScope.launch {
                                            if (isActionInProgress) return@launch
                                            isActionInProgress = true
                                            try {
                                                val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                                                val diffMinutes = calculateTimeDifferenceInMinutes(now, ride.departureTime!!)
                                                threshold = if (ride.direction == RideDirection.TO_WORK) 180 else 60
                                                if (diffMinutes < threshold) {
                                                    showTooLateDialog = true
                                                    return@launch
                                                }

                                                rideRepository.deleteRide(ride.rideId)
                                                refreshRides()
                                            } catch (e: Exception) {
                                                error = e.message
                                            } finally {
                                                isActionInProgress = false
                                            }
                                        }
                                    }) {
                                        Text("Cancel Ride")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(onClick = {
                                    selectedRide = ride
                                    coroutineScope.launch {
                                        val names = mutableMapOf<String, String>()
                                        val pickupMap = mutableMapOf<String, PickupStop>()

                                        ride.pickupStops.forEach { stop ->
                                            userRepository.getUser(stop.passengerId)?.let { user ->
                                                names[stop.passengerId] = user.name
                                                pickupMap[stop.passengerId] = stop
                                            }
                                        }

                                        passengerNames = names
                                        selectedPassengerStop = pickupMap.values.firstOrNull()
                                        showDetailsDialog = true
                                    }
                                }) {
                                    Text("Passenger Details")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid?fromLogin=false") {
                    popUpTo("intermediate/$uid?fromLogin=false") { inclusive = false }
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Home")
        }
    }

    if (showDetailsDialog && selectedRide != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Ride Details") },
            text = {
                Column {
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedPassengerStop?.let { passengerNames[it.passengerId] ?: "Unknown" } ?: "Select Passenger",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Passengers") },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Dropdown")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val stops = selectedRide?.pickupStops.orEmpty()
                            if (stops.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No Passengers") },
                                    onClick = {},
                                    enabled = false
                                )
                            } else {
                                stops.forEach { stop ->
                                    val name = passengerNames[stop.passengerId] ?: "Unknown"
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            selectedPassengerStop = stop
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    selectedPassengerStop?.let { stop ->
                        val isWorkbound = selectedRide!!.direction == RideDirection.TO_WORK
                        Text("Pickup Location: ${stop.location.name}")
                        Text(if (isWorkbound) "Pickup Time: ${stop.pickupTime}" else "Dropoff Time: ${stop.dropoffTime}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                                    val diffMinutes = calculateTimeDifferenceInMinutes(now, selectedRide!!.departureTime!!)
                                    threshold = if (isWorkbound) 180 else 60
                                    if (diffMinutes < threshold) {
                                        innerTooLateDialog = true
                                        return@launch
                                    }

                                    rideRepository.removePassengerFromRide(
                                        rideId = selectedRide!!.rideId,
                                        passengerId = stop.passengerId,
                                        rideCanceledForOnePassenger = true
                                    )

                                    val request = requestRepository.getRequestsByPassenger(stop.passengerId)
                                        .firstOrNull {
                                            it.rideId == selectedRide!!.rideId && it.status.name == "ACCEPTED"
                                        }

                                    request?.let {
                                        rideRepository.declineRideRequest(it.rideId, it.requestId)
                                    }

                                    refreshRides()
                                    showDetailsDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                        ) {
                            Text("Cancel Passenger", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showTooLateDialog || innerTooLateDialog) {
        AlertDialog(
            onDismissRequest = {
                showTooLateDialog = false
                innerTooLateDialog = false
            },
            title = { Text("Too Late") },
            text = {
                Text("This action is not allowed. You cannot cancel a ride less than $threshold minutes before departure time.")
            },
            confirmButton = {
                Button(onClick = {
                    showTooLateDialog = false
                    innerTooLateDialog = false
                }) {
                    Text("OK")
                }
            }
        )
    }
}
