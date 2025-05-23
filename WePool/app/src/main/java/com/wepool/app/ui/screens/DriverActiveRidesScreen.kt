package com.wepool.app.ui.screens

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.RideNavigationServiceController
import com.wepool.app.ui.screens.components.ActiveRidesFilterCard
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.RideMapDialog
import com.wepool.app.ui.screens.components.RidePassengerDetailsDialog
import com.wepool.app.ui.screens.components.filterRides
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DriverActiveRidesScreen(uid: String, navController: NavController, rideId: String? = null) {
    val context = LocalContext.current
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val requestRepository = RepositoryProvider.provideRideRequestRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionState =
        rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isActionInProgress by remember { mutableStateOf(false) }
    var selectedRide by remember { mutableStateOf<Ride?>(null) }
    var selectedPassengerUser by remember { mutableStateOf<User?>(null) }
    var passengerNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedPassengerStop by remember { mutableStateOf<PickupStop?>(null) }
    var showTooLateDialog by remember { mutableStateOf(false) }
    var innerTooLateDialog by remember { mutableStateOf(false) }
    var threshold by remember { mutableStateOf(0) }
    var rideForMapDialog by remember { mutableStateOf<Ride?>(null) }

    var showDateRangePicker by remember { mutableStateOf(false) }
    var showTimeRangePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf<RideDirection?>(null) }
    var directionMenuExpanded by remember { mutableStateOf(false) }
    val directionOptions =
        listOf(RideDirection.TO_HOME to "To Home", RideDirection.TO_WORK to "To Work")


    fun refreshRides() {
        coroutineScope.launch {
            try {
                loading = true
                error = null
                val allRides = driverRepository.getActiveRidesForDriver(uid)
                rides =
                    filterRides(allRides, startDate, endDate, startTime, endTime, selectedDirection)
            } catch (e: Exception) {
                error = "❌ שגיאה בטעינת נסיעות: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    val calendar = remember { Calendar.getInstance() }

    LaunchedEffect(showDateRangePicker) {
        if (showDateRangePicker) {
            DatePickerDialog(
                context,
                { _, startYear, startMonth, startDay ->
                    val proposedStart =
                        String.format("%04d-%02d-%02d", startYear, startMonth + 1, startDay)

                    DatePickerDialog(
                        context,
                        { _, endYear, endMonth, endDay ->
                            val proposedEnd =
                                String.format("%04d-%02d-%02d", endYear, endMonth + 1, endDay)

                            if (proposedEnd >= proposedStart) {
                                startDate = proposedStart
                                endDate = proposedEnd
                            } else {
                                Toast.makeText(
                                    context,
                                    "❗ End date must be after start date",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startDate = ""
                                endDate = ""
                            }

                            showDateRangePicker = false
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    LaunchedEffect(showTimeRangePicker) {
        if (showTimeRangePicker) {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    startTime = String.format("%02d:%02d", hour, minute)
                    TimePickerDialog(
                        context,
                        { _, hour2, minute2 ->
                            endTime = String.format("%02d:%02d", hour2, minute2)
                            showTimeRangePicker = false
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    DriverActiveRidesContent(
        uid = uid,
        rides = rides,
        rideId = rideId,
        loading = loading,
        error = error,
        navController = navController,
        startDate = startDate,
        endDate = endDate,
        startTime = startTime,
        endTime = endTime,
        selectedDirection = selectedDirection,
        directionMenuExpanded = directionMenuExpanded,
        directionOptions = directionOptions,
        showDateRangePicker = { showDateRangePicker = true },
        showTimeRangePicker = { showTimeRangePicker = true },
        onClearDateRange = {
            startDate = ""
            endDate = ""
        },
        onClearTimeRange = {
            startTime = ""
            endTime = ""
        },
        onClearDirection = {
            selectedDirection = null
        },
        onDirectionSelected = {
            selectedDirection = it
            directionMenuExpanded = false
        },
        onDirectionMenuExpand = { directionMenuExpanded = true },
        onDirectionMenuDismiss = { directionMenuExpanded = false },
        onRefreshClicked = { refreshRides() },
        onStartRideClicked = { ride ->
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
        },
        onCancelRideClicked = { ride ->
            coroutineScope.launch {
                if (isActionInProgress) return@launch
                isActionInProgress = true
                try {
                    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    val diffMinutes =
                        rideRepository.calculateTimeDifferenceInMinutes(now, ride.departureTime!!)
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
        },
        onShowMapClicked = { ride -> rideForMapDialog = ride },
        onPassengerDetailsClicked = { ride ->
            selectedRide = ride
            coroutineScope.launch {
                val names = mutableMapOf<String, String>()
                ride.pickupStops.forEach { stop ->
                    userRepository.getUser(stop.passengerId)?.let { user ->
                        names[stop.passengerId] = user.name
                    }
                }
                passengerNames = names
                showDetailsDialog = true
            }
        }
    )

    // --- Map Dialog ---
    rideForMapDialog?.let { ride ->
        RideMapDialog(ride = ride, onDismiss = { rideForMapDialog = null })
    }

    // --- Passenger Details Dialog ---
    RidePassengerDetailsDialog(
        showDialog = showDetailsDialog,
        ride = selectedRide,
        passengerNames = passengerNames,
        userRepository = userRepository,
        rideRepository = rideRepository,
        requestRepository = requestRepository,
        onDismiss = {
            showDetailsDialog = false
            selectedPassengerStop = null
            selectedPassengerUser = null
        },
        onPassengerRemoved = {
            refreshRides()
            showDetailsDialog = false
        }
    )

    // --- Too Late Dialog ---
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

@Composable
fun DriverActiveRidesContent(
    uid: String,
    rides: List<Ride>,
    rideId: String?,
    loading: Boolean,
    error: String?,
    navController: NavController,
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String,
    selectedDirection: RideDirection?,
    directionMenuExpanded: Boolean,
    directionOptions: List<Pair<RideDirection, String>>,
    showDateRangePicker: () -> Unit,
    showTimeRangePicker: () -> Unit,
    onClearDateRange: () -> Unit,
    onClearTimeRange: () -> Unit,
    onClearDirection: () -> Unit,
    onDirectionSelected: (RideDirection) -> Unit,
    onDirectionMenuExpand: () -> Unit,
    onDirectionMenuDismiss: () -> Unit,
    onRefreshClicked: () -> Unit,
    onStartRideClicked: (Ride) -> Unit,
    onCancelRideClicked: (Ride) -> Unit,
    onShowMapClicked: (Ride) -> Unit,
    onPassengerDetailsClicked: (Ride) -> Unit
) {

    Box(modifier = Modifier.fillMaxSize()) {

        // תוכן הגלילה
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 96.dp), // להשאיר מקום לכפתורים
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Your Active Rides",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            ActiveRidesFilterCard(
                startDate = startDate,
                endDate = endDate,
                startTime = startTime,
                endTime = endTime,
                selectedDirection = selectedDirection,
                directionMenuExpanded = directionMenuExpanded,
                directionOptions = directionOptions,
                onShowDateRangePicker = showDateRangePicker,
                onShowTimeRangePicker = showTimeRangePicker,
                onClearDateRange = onClearDateRange,
                onClearTimeRange = onClearTimeRange,
                onClearDirection = onClearDirection,
                onDirectionSelected = onDirectionSelected,
                onDirectionMenuExpand = onDirectionMenuExpand,
                onDirectionMenuDismiss = onDirectionMenuDismiss,
                onApplyFilter = onRefreshClicked
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                rides.isEmpty() -> Text("Click 'Apply Filter' to search for active rides.")
                else -> {
                    val filteredRides =
                        if (!rideId.isNullOrEmpty()) rides.filter { it.rideId == rideId } else rides

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredRides) { ride ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("From: ${ride.startLocation.name}")
                                    Text("To: ${ride.destination.name}")
                                    Text("Date: ${ride.date}")
                                    Text("Departure Time: ${ride.departureTime}")
                                    Text("Arrival Time: ${ride.arrivalTime}")
                                    Text("Stops on the way: ${ride.pickupStops.size}")
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onStartRideClicked(ride) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2E7D32), //ירוק כהה
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Start Ride")
                                        }

                                        Button(
                                            onClick = { onCancelRideClicked(ride) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFC62828), // אדום כהה
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Cancel Ride")
                                        }

                                        OutlinedButton(
                                            onClick = { onShowMapClicked(ride) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF039BE5), // תכלת כהה
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Show Map")
                                        }

                                        OutlinedButton(
                                            onClick = { onPassengerDetailsClicked(ride) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFBC02D), // צהוב כהה
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Passenger Details")
                                        }
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
}