package com.wepool.app.ui.screens.driverScreens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wepool.app.R
import com.wepool.app.data.model.enums.RequestStatus
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.ride.RideRequest
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.RideMapDialog
import kotlinx.coroutines.launch

@Composable
fun DriverRequestsScreen(uid: String, navController: NavController, filterRideId: String? = null) {
    val requestStatuses = listOf("All", "Pending", "Accepted", "Declined")
    var expanded by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(requestStatuses[0]) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<RideRequest>>(emptyList()) }
    var passengerNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val userRepo = RepositoryProvider.provideUserRepository()
    val requestRepo = RepositoryProvider.provideRideRequestRepository()
    val rideRepo = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    var selectedRequest by remember { mutableStateOf<RideRequest?>(null) }
    var selectedRideDirection by remember { mutableStateOf<RideDirection?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var rideForMapDialog by remember { mutableStateOf<Pair<Ride, PickupStop?>?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    fun refresh() {
        coroutineScope.launch {
            loading = true
            error = null
            try {
                val allRequests = requestRepo.getRequestsByDriver(uid)
                val filteredRequests = allRequests.filter {
                    (selectedStatus == "All" || it.status.name.equals(
                        selectedStatus,
                        ignoreCase = true
                    )) &&
                            (filterRideId == null || it.rideId == filterRideId)
                }.filterNot { it.status == RequestStatus.CANCELLED }

                results = filteredRequests
                val namesMap = mutableMapOf<String, String>()
                val seenIds = mutableSetOf<String>()

                filteredRequests.forEach { request ->
                    try {
                        if (seenIds.add(request.passengerId)) {
                            userRepo.getUser(request.passengerId)?.let { user ->
                                namesMap[request.passengerId] = user.name
                            }
                        }
                        val ride = rideRepo.getRide(request.rideId)
                        ride?.passengers?.forEach { passengerId ->
                            if (seenIds.add(passengerId)) {
                                userRepo.getUser(passengerId)?.let { user ->
                                    namesMap[passengerId] = user.name
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "DriverRequests",
                            "\u274C Error while processing request: ${e.message}"
                        )
                    }
                }

                passengerNames = namesMap
                Log.d("DriverRequests", "\u2705 Refreshed ${results.size} requests")

            } catch (e: Exception) {
                error = "\u274C Failed to refresh: ${e.message}"
                Log.e("DriverRequests", "\u274C Refresh error: ${e.message}", e)
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
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ride Requests", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Filter by status",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth(0.75f)
                                        .height(56.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = selectedStatus,
                                            modifier = Modifier.align(Alignment.Center),
                                            fontSize = 18.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .size(28.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                ) {
                                    requestStatuses.forEach { status ->
                                        DropdownMenuItem(
                                            text = {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(status)
                                                }
                                            },
                                            onClick = {
                                                selectedStatus = status
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    hasSearched = true
                                    refresh()
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(48.dp)
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


                    Spacer(modifier = Modifier.height(32.dp))

                    when {
                        !hasSearched -> {
                            Text("Please select a filter and press Refresh to load ride requests.")
                        }

                        loading -> CircularProgressIndicator()
                        error != null -> Text(
                            error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )

                        results.isEmpty() -> Text("No matching requests found.")
                        else -> LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(results) { request ->
                                val ride =
                                    remember { mutableStateOf<Ride?>(null) }

                                LaunchedEffect(request.rideId) {
                                    ride.value = rideRepo.getRide(request.rideId)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        ride.value?.let { r ->
                                            Text("Direction: ${if (r.direction == RideDirection.TO_HOME) "To Home" else "To Work"}")

                                            val locationLabel =
                                                if (r.direction == RideDirection.TO_WORK) "Pickup Location" else "Dropoff Location"
                                            Text("$locationLabel: ${request.pickupLocation.name}")

                                            val otherPassengers =
                                                r.passengers.filterNot { it == request.passengerId }
                                            val requesterName =
                                                passengerNames[request.passengerId] ?: "Unknown"
                                            val others = if (otherPassengers.isNotEmpty()) {
                                                otherPassengers.joinToString(", ") { pid ->
                                                    passengerNames[pid] ?: "Unknown"
                                                }
                                            } else "None"

                                            Text("Date: ${r.date} | Departure: ${r.departureTime} | Arrival: ${r.arrivalTime}")
                                            Text("Request by: $requesterName")
                                            Text("Other passengers: $others")
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        when (request.status) {
                                            RequestStatus.PENDING -> {
                                                val statusColor = Color(0xFFFFC107)
                                                Button(
                                                    onClick = {
                                                        selectedRequest = request
                                                        selectedRideDirection =
                                                            ride.value?.direction
                                                        showDialog = true
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = statusColor
                                                    )
                                                ) {
                                                    Text(text = "PENDING")
                                                }
                                            }

                                            else -> {
                                                val statusColor = when (request.status) {
                                                    RequestStatus.ACCEPTED -> Color(0xFF4CAF50)
                                                    RequestStatus.DECLINED -> Color(0xFFF44336)
                                                    else -> Color.Gray
                                                }
                                                Text(
                                                    text = request.status.name,
                                                    color = statusColor,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    rideForMapDialog?.let { (ride, stop) ->
                        RideMapDialog(
                            ride = ride,
                            extraPickupStop = stop,
                            onDismiss = { rideForMapDialog = null })
                    }
                    BottomNavigationButtons(
                        uid = uid,
                        navController = navController,
                        showBackButton = true,
                        showHomeButton = true
                    )
                }
            }
        }

        if (showDialog && selectedRequest != null) {
            val isToWork = selectedRideDirection == RideDirection.TO_WORK
            val pickupTime = selectedRequest!!.detourEvaluationResult.pickupLocation?.pickupTime
            val dropoffTime = selectedRequest!!.detourEvaluationResult.pickupLocation?.dropoffTime
            val updatedRefTime = selectedRequest!!.detourEvaluationResult.updatedReferenceTime

            val stop = selectedRequest!!.detourEvaluationResult.pickupLocation

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Request Details") },
                    text = {
                        Column {
                            Text(if (isToWork) "Pickup Time: $pickupTime" else "Dropoff Time: $dropoffTime")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(if (isToWork) "New Departure Time: $updatedRefTime" else "New Arrival Time: $updatedRefTime")

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val ride = rideRepo.getRide(selectedRequest!!.rideId)
                                        if (ride != null) {
                                            rideForMapDialog = ride to stop
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF03A9F4
                                    )
                                )
                            ) {
                                Text("Show Map")
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val ride = rideRepo.getRide(selectedRequest!!.rideId)
                                            if (ride == null) {
                                                Log.w("RideApproval", "\u26a0 Ride not found: ${selectedRequest!!.rideId}")
                                                showDialog = false
                                                return@launch
                                            }

                                            val candidate = RideCandidate(
                                                ride = ride,
                                                detourEvaluationResult = selectedRequest!!.detourEvaluationResult
                                            )

                                            val success = rideRepo.approvePassengerRequest(
                                                candidate = candidate,
                                                requestId = selectedRequest!!.requestId,
                                                passengerId = selectedRequest!!.passengerId
                                            )

                                            if (success) {
                                                Log.d("RideApproval", "✅ Approved request ${selectedRequest!!.requestId}")
                                                refresh()
                                            } else {
                                                Log.w("RideApproval", "⚠ Approval failed for ${selectedRequest!!.requestId}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RideApproval", "❌ Error during approval: ${e.message}", e)
                                        } finally {
                                            showDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.size(80.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.approve_hand_like_svgrepo_com),
                                        contentDescription = "Approve",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Approve", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val success = rideRepo.declineRideRequest(
                                                rideId = selectedRequest!!.rideId,
                                                requestId = selectedRequest!!.requestId
                                            )
                                            if (success) {
                                                Log.d("RideDecline", "✅ Declined request ${selectedRequest!!.requestId}")
                                                refresh()
                                            } else {
                                                Log.w("RideDecline", "⚠ Decline failed for ${selectedRequest!!.requestId}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("RideDecline", "❌ Error during decline: ${e.message}", e)
                                        } finally {
                                            showDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.size(80.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.dislike_down_hand_svgrepo_com),
                                        contentDescription = "Decline",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Decline", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}