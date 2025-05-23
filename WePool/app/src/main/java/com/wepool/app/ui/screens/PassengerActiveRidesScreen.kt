package com.wepool.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun PassengerActiveRidesScreen(uid: String, navController: NavController, rideId: String? = null) {
    val context = LocalContext.current
    val passengerRepository = RepositoryProvider.providePassengerRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val requestRepository = RepositoryProvider.provideRideRequestRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()

    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var filteredRides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var hasFilterBeenApplied by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf<RideDirection?>(null) }
    var directionMenuExpanded by remember { mutableStateOf(false) }

    var showDateRangePicker by remember { mutableStateOf(false) }
    var showTimeRangePicker by remember { mutableStateOf(false) }

    var showDriverDialog by remember { mutableStateOf(false) }
    var selectedRide by remember { mutableStateOf<Ride?>(null) }
    var selectedDriver by remember { mutableStateOf<User?>(null) }

    val directionOptions = listOf(RideDirection.TO_HOME to "To Home", RideDirection.TO_WORK to "To Work")

    val calendar = remember { Calendar.getInstance() }

    fun refreshRides() {
        coroutineScope.launch {
            loading = true
            error = null
            try {
                val allRides = passengerRepository.getActiveRidesForPassenger(uid)
                rides = allRides
                filteredRides = filterRides(allRides, startDate, endDate, startTime, endTime, selectedDirection)
                hasFilterBeenApplied = true
            } catch (e: Exception) {
                error = "❌ Failed to load rides: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(showDateRangePicker) {
        if (showDateRangePicker) {
            DatePickerDialog(context, { _, startYear, startMonth, startDay ->
                val proposedStart = "%04d-%02d-%02d".format(startYear, startMonth + 1, startDay)
                DatePickerDialog(context, { _, endYear, endMonth, endDay ->
                    val proposedEnd = "%04d-%02d-%02d".format(endYear, endMonth + 1, endDay)
                    if (proposedEnd >= proposedStart) {
                        startDate = proposedStart
                        endDate = proposedEnd
                    } else {
                        startDate = ""
                        endDate = ""
                    }
                    showDateRangePicker = false
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    LaunchedEffect(showTimeRangePicker) {
        if (showTimeRangePicker) {
            TimePickerDialog(context, { _, hour, minute ->
                startTime = "%02d:%02d".format(hour, minute)
                TimePickerDialog(context, { _, hour2, minute2 ->
                    endTime = "%02d:%02d".format(hour2, minute2)
                    showTimeRangePicker = false
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 96.dp)
        ) {
            Text("Your Active Rides", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))

            ActiveRidesFilterCard(
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                selectedDirection = selectedDirection,
                directionMenuExpanded = directionMenuExpanded,
                directionOptions = directionOptions,
                onShowDateRangePicker = { showDateRangePicker = true },
                onShowTimeRangePicker = { showTimeRangePicker = true },
                onClearDateRange = { startDate = ""; endDate = "" },
                onClearTimeRange = { startTime = ""; endTime = "" },
                onClearDirection = { selectedDirection = null },
                onDirectionSelected = { selectedDirection = it; directionMenuExpanded = false },
                onDirectionMenuExpand = { directionMenuExpanded = true },
                onDirectionMenuDismiss = { directionMenuExpanded = false },
                onApplyFilter = {
                    refreshRides()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                !hasFilterBeenApplied -> Text("Please select filter options and press Apply Filter.")
                filteredRides.isEmpty() -> Text("No active rides found for selected criteria.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredRides.filter { rideId == null || it.rideId == rideId }) { ride ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("From: ${ride.startLocation.name}")
                                Text("To: ${ride.destination.name}")
                                Text("Date: ${ride.date}")
                                if (ride.direction == RideDirection.TO_WORK) {
                                    Text("Arrival: ${ride.arrivalTime}")
                                    Text("Pickup: ${rideRepository.getPickupTimeForPassenger(ride, uid)}")
                                } else {
                                    Text("Departure: ${ride.departureTime}")
                                    Text("Dropoff: ${rideRepository.getDropoffTimeForPassenger(ride, uid)}")
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    rideRepository.removePassengerFromRide(ride.rideId, uid, false)
                                                    requestRepository.getRequestsByPassenger(uid).firstOrNull {
                                                        it.rideId == ride.rideId && it.status.name == "ACCEPTED"
                                                    }?.let {
                                                        rideRepository.cancelRideRequest(ride.rideId, it.requestId)
                                                    }
                                                    refreshRides()
                                                } catch (e: Exception) {
                                                    Log.e("PassengerCancel", "Error: ${e.message}")
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFC62828), // אדום כהה
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Cancel Ride")
                                    }

                                    Button(
                                        onClick = {
                                            selectedRide = ride
                                            showDriverDialog = true
                                            coroutineScope.launch {
                                                selectedDriver = userRepository.getUser(ride.driverId)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF039BE5), // תכלת
                                            contentColor = Color.White // טקסט לבן
                                        )
                                    ) {
                                        Text("Driver Details")
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            BottomNavigationButtons(
                uid = uid,
                rideId = rideId,
                navController = navController,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                showBackButton = true,
                showHomeButton = true
            )
        }
    }

    if (showDriverDialog && selectedDriver != null) {
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = {
                showDriverDialog = false
                selectedDriver = null
            },
            title = { Text("Driver Details") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Name: ${selectedDriver!!.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: ${selectedDriver!!.email}")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${selectedDriver!!.phoneNumber}")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Driver",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Call: ${selectedDriver!!.phoneNumber}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDriverDialog = false
                    selectedDriver = null
                }) {
                    Text("Close")
                }
            }
        )
    }

}