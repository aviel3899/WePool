package com.wepool.app.ui.screens.adminScreens.rides

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wepool.app.R
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.ui.screens.components.StatusLabel

@Composable
fun RideCard(
    ride: Ride,
    selectedUserUid: String?,
    onShowMapClicked: (Ride) -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    var showPassengerDialog by remember { mutableStateOf(false) }
    var showDriverDialog by remember { mutableStateOf(false) }

    val roleIcon = when {
        selectedUserUid == null -> null
        ride.driverId == selectedUserUid -> R.drawable.steering_wheel_car_svgrepo_com
        ride.pickupStops.any { it.passengerId == selectedUserUid } -> R.drawable.seat_belt_svgrepo_com
        else -> null
    }

    val directionIcon = when (ride.direction) {
        RideDirection.TO_WORK -> R.drawable.work_is_money_svgrepo_com
        RideDirection.TO_HOME -> R.drawable.home_house_svgrepo_com
        else -> null
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {

                StatusLabel(
                    active = ride.active,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "${ride.startLocation.name} ➝ ${ride.destination.name}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        roleIcon?.let {
                            Image(
                                painter = painterResource(id = it),
                                contentDescription = "Role",
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        if (directionIcon != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.arrow_forward_long_svgrepo_com),
                                    contentDescription = "Direction Arrow",
                                    modifier = Modifier.size(72.dp),
                                )
                                Image(
                                    painter = painterResource(id = directionIcon),
                                    contentDescription = "Direction Icon",
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Date: ${ride.date}")
                    Text("Departure Time: ${ride.departureTime ?: "N/A"}")
                    Text("Arrival Time: ${ride.arrivalTime ?: "N/A"}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showDriverDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.steering_wheel_car_svgrepo_com),
                                contentDescription = "Driver",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        DriverDetailsDialog(
                            showDialog = showDriverDialog,
                            ride = ride,
                            onDismiss = { showDriverDialog = false }
                        )

                        IconButton(
                            onClick = { showPassengerDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.seat_belt_svgrepo_com),
                                contentDescription = "Passenger",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        PassengerDetailsDialog(
                            showDialog = showPassengerDialog,
                            ride = ride,
                            onDismiss = { showPassengerDialog = false }
                        )

                        IconButton(
                            onClick = { onShowMapClicked(ride) },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.map_svgrepo_com),
                                contentDescription = "Show Map",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDetails = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Details",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Ride Details")
                }
            },
            text = {
                Column {
                    Text("From: ${ride.startLocation.name}")
                    Text("To: ${ride.destination.name}")
                    Text("Date: ${ride.date}")
                    Text("Departure Time: ${ride.departureTime}")
                    Text("Arrival Time: ${ride.arrivalTime}")
                    Text("Available Seats: ${(ride.availableSeats - (ride.occupiedSeats)).coerceAtLeast(0)}")
                    Text("Stops: ${ride.pickupStops.size}")
                    Text("Detour: ${ride.currentDetourMinutes} min (max ${ride.maxDetourMinutes})")
                    Text("Direction: ${ride.direction?.name}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close")
                }
            }
        )
    }
}
