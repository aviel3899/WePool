package com.wepool.app.ui.screens.passengerScreens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.enums.ride.RideFilterFields
import com.wepool.app.data.model.enums.ride.RideSortFields
import com.wepool.app.data.model.enums.ride.RideSortFieldsWithOrder
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableCard
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

    var filters by remember { mutableStateOf(RideSearchFilters()) }
    var selectedFilters by remember { mutableStateOf<List<RideFilterFields>>(emptyList()) }
    var searchTriggered by remember { mutableStateOf(false) }

    var showDriverDialog by remember { mutableStateOf(false) }
    var selectedRide by remember { mutableStateOf<Ride?>(null) }
    var selectedDriver by remember { mutableStateOf<User?>(null) }

    fun refreshRides() {
        coroutineScope.launch {
            loading = true
            error = null
            try {
                val allRides = passengerRepository.getActiveRidesForPassenger(uid)
                rides = allRides
                filteredRides = allRides.filter {
                    (filters.dateFrom == null || it.date >= filters.dateFrom!!) &&
                            (filters.dateTo == null || it.date <= filters.dateTo!!) &&
                            (filters.timeFrom == null || it.departureTime!! >= filters.timeFrom!!) &&
                            (filters.timeTo == null || it.arrivalTime!! <= filters.timeTo!!) &&
                            (filters.direction == null || it.direction == filters.direction)
                }
                hasFilterBeenApplied = true
            } catch (e: Exception) {
                error = "❌ Failed to load rides: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 96.dp)
                ) {
                    Text(
                        "Your Active Rides",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExpandableCard(
                        filters = filters,
                        selectedFilters = selectedFilters,
                        onFiltersChanged = { updated ->
                            if (updated is RideSearchFilters) filters = updated
                        },
                        availableFilters = listOf(
                            RideFilterFields.DATE_RANGE,
                            RideFilterFields.TIME_RANGE,
                            RideFilterFields.DIRECTION
                        ),
                        rideAvailableSortFields = listOf(
                            RideSortFields.DATE,
                            RideSortFields.DEPARTURE_TIME,
                            RideSortFields.ARRIVAL_TIME
                        ),
                        selectedSortFields = filters.sortFields,
                        onSortFieldsChanged = {
                            filters =
                                filters.copy(sortFields = it.filterIsInstance<RideSortFieldsWithOrder>())
                        },
                        onSelectedFiltersChanged = {
                            selectedFilters = it.filterIsInstance<RideFilterFields>()
                        },
                        onSearchClicked = { refreshRides() },
                        onSearchTriggeredChanged = { searchTriggered = false },
                        onCleanAllClicked = {
                            filters = RideSearchFilters()
                            selectedFilters = emptyList()
                            searchTriggered = true
                            filteredRides = emptyList()

                            coroutineScope.launch {
                                searchTriggered = false
                                kotlinx.coroutines.delay(50)
                                searchTriggered = true
                            }
                        },
                        showCompanyName = false,
                        showUserName = false,
                        showAvailableSeats = false,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        loading -> CircularProgressIndicator()
                        error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                        !hasFilterBeenApplied -> Text("Please select filter options and press Search.")
                        filteredRides.isEmpty() -> Text("No active rides found for selected criteria.")
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredRides.filter { rideId == null || it.rideId == rideId }) { ride ->
                                val rideRequest by produceState<RideRequest?>(
                                    initialValue = null,
                                    ride
                                ) {
                                    value = try {
                                        requestRepository.getRequestsByPassenger(uid)
                                            .firstOrNull { it.rideId == ride.rideId }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                val passengerDropoffLocation = remember(ride.pickupStops, uid) {
                                    ride.pickupStops.firstOrNull { it.passengerId == uid }?.location?.name
                                }

                                val currentTime = Calendar.getInstance().timeInMillis
                                val rideStartTime = ride.departureTime?.let {
                                    val (hour, minute) = it.split(":").map(String::toInt)
                                    Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                    }.timeInMillis
                                }

                                val canCancelRide = when (ride.direction) {
                                    RideDirection.TO_WORK -> rideStartTime?.let { currentTime + 3600000 < it } == true
                                    RideDirection.TO_HOME -> rideStartTime?.let { currentTime + 600000 < it } == true
                                    else -> false
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        if (ride.direction == RideDirection.TO_WORK) {
                                            Text("Pickup location: ${rideRequest?.pickupLocation?.name ?: "Loading..."}")
                                            Text("To: ${ride.destination.name}")
                                        } else {
                                            Text("From: ${ride.startLocation.name}")
                                            Text("Dropoff location: ${passengerDropoffLocation ?: "Loading..."}")
                                        }

                                        Text("Date: ${ride.date}")
                                        if (ride.direction == RideDirection.TO_WORK) {
                                            Text("Arrival: ${ride.arrivalTime}")
                                            Text(
                                                "Pickup: ${
                                                    rideRepository.getPickupTimeForPassenger(
                                                        ride,
                                                        uid
                                                    )
                                                }"
                                            )
                                        } else {
                                            Text("Departure: ${ride.departureTime}")
                                            Text(
                                                "Dropoff: ${
                                                    rideRepository.getDropoffTimeForPassenger(
                                                        ride,
                                                        uid
                                                    )
                                                }"
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    if (canCancelRide) {
                                                        coroutineScope.launch {
                                                            try {
                                                                rideRepository.removePassengerFromRide(
                                                                    ride.rideId,
                                                                    uid,
                                                                    false
                                                                )
                                                                requestRepository.getRequestsByPassenger(
                                                                    uid
                                                                ).firstOrNull {
                                                                    it.rideId == ride.rideId && it.status.name == "ACCEPTED"
                                                                }?.let {
                                                                    rideRepository.cancelRideRequest(
                                                                        ride.rideId,
                                                                        it.requestId
                                                                    )
                                                                }
                                                                refreshRides()
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "PassengerCancel",
                                                                    "Error: ${e.message}"
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Too late to cancel the ride",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(
                                                        0xFFC62828
                                                    ), contentColor = Color.White
                                                )
                                            ) {
                                                Text("Cancel Ride")
                                            }

                                            Button(
                                                onClick = {
                                                    selectedRide = ride
                                                    showDriverDialog = true
                                                    coroutineScope.launch {
                                                        selectedDriver =
                                                            userRepository.getUser(ride.driverId)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(
                                                        0xFF039BE5
                                                    ), contentColor = Color.White
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
                        navController = navController,
                        showBackButton = true,
                        showHomeButton = true
                    )
                }

                if (showDriverDialog && selectedDriver != null) {
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
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${selectedDriver!!.phoneNumber}")
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
                                        "Call: ${selectedDriver!!.phoneNumber}",
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
        }
    }
}
