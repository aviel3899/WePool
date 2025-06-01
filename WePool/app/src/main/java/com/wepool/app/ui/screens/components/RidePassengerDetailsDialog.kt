package com.wepool.app.ui.screens.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.data.repository.interfaces.IRideRequestRepository
import com.wepool.app.data.repository.interfaces.IUserRepository
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun RidePassengerDetailsDialog(
    showDialog: Boolean,
    ride: Ride?,
    passengerNames: Map<String, String>,
    userRepository: IUserRepository,
    rideRepository: IRideRepository,
    requestRepository: IRideRequestRepository,
    onDismiss: () -> Unit,
    onPassengerRemoved: () -> Unit
) {
    if (!showDialog || ride == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPassengerStop by remember { mutableStateOf<PickupStop?>(null) }
    var selectedPassengerUser by remember { mutableStateOf<User?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var innerTooLateDialog by remember { mutableStateOf(false) }
    var threshold by remember { mutableStateOf(0) }
    val textFieldWidth = remember { mutableStateOf(0) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ride Details") },
            text = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                textFieldWidth.value = coordinates.size.width
                            }) {
                        OutlinedTextField(
                            value = selectedPassengerStop?.let {
                                passengerNames[it.passengerId] ?: "Unknown"
                            } ?: "Select Passenger",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Passengers") },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.width(with(LocalDensity.current) { textFieldWidth.value.toDp() })
                        ) {
                            val stops = ride.pickupStops
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
                                            coroutineScope.launch {
                                                try {
                                                    selectedPassengerUser =
                                                        userRepository.getUser(stop.passengerId)
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "RideDialog",
                                                        "Failed to load user: ${e.message}"
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    selectedPassengerStop?.let { stop ->
                        val isWorkbound = ride.direction == RideDirection.TO_WORK
                        Text(if (isWorkbound) "Pickup Location: ${stop.location.name}" else "Dropoff Location: ${stop.location.name}")
                        Text(if (isWorkbound) "Pickup Time: ${stop.pickupTime}" else "Dropoff Time: ${stop.dropoffTime}")

                        selectedPassengerUser?.let { user ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${user.phoneNumber}")
                                        }
                                        context.startActivity(intent)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Passenger",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Call: ${user.phoneNumber}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val rideDateParts = ride.date.split("-")
                                            .map { it.toInt() } // [dd, MM, yyyy]
                                        val rideTimeParts =
                                            ride.departureTime!!.split(":").map { it.toInt() }

                                        val rideCalendar = Calendar.getInstance().apply {
                                            set(Calendar.DAY_OF_MONTH, rideDateParts[0])
                                            set(Calendar.MONTH, rideDateParts[1] - 1)
                                            set(Calendar.YEAR, rideDateParts[2])
                                            set(Calendar.HOUR_OF_DAY, rideTimeParts[0])
                                            set(Calendar.MINUTE, rideTimeParts[1])
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }

                                        val now = Calendar.getInstance()
                                        val diffMillis =
                                            rideCalendar.timeInMillis - now.timeInMillis
                                        val diffMinutes = diffMillis / (60 * 1000)

                                        threshold = if (isWorkbound) 60 else 10
                                        if (diffMinutes < threshold) {
                                            innerTooLateDialog = true
                                            return@launch
                                        }

                                        rideRepository.removePassengerFromRide(
                                            rideId = ride.rideId,
                                            passengerId = stop.passengerId,
                                            rideCanceledForOnePassenger = true
                                        )

                                        requestRepository.getRequestsByPassenger(stop.passengerId)
                                            .firstOrNull {
                                                it.rideId == ride.rideId && it.status.name == "ACCEPTED"
                                            }?.let {
                                                rideRepository.declineRideRequest(
                                                    it.rideId,
                                                    it.requestId
                                                )
                                            }

                                        onPassengerRemoved()
                                    } catch (e: Exception) {
                                        Log.e(
                                            "RideDialog",
                                            "❌ Failed to cancel passenger: ${e.message}"
                                        )
                                    }
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        )

        if (innerTooLateDialog) {
            AlertDialog(
                onDismissRequest = { innerTooLateDialog = false },
                title = { Text("Too Late") },
                text = { Text("You cannot cancel a ride less than $threshold minutes before departure.") },
                confirmButton = {
                    Button(onClick = { innerTooLateDialog = false }) {
                        Text("OK")
                    }
                }

            )
        }
    }
}