package com.wepool.app.ui.screens.rideHistory

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider

@Composable
fun RideHistoryCard(ride: Ride) {
    val userRepository = RepositoryProvider.provideUserRepository()

    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedUserRole by remember { mutableStateOf<UserRole?>(null) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var passengerMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var driverUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(ride.driverId) {
        try {
            driverUser = userRepository.getUser(ride.driverId)
        } catch (e: Exception) {
            Log.e("RideCard", "❌ Error loading driver: ${e.message}")
        }
    }

    LaunchedEffect(ride.passengers) {
        val map = mutableMapOf<String, User>()
        ride.passengers.forEach { uid ->
            try {
                userRepository.getUser(uid)?.let { map[uid] = it }
            } catch (e: Exception) {
                Log.e("RideCard", "❌ Error loading passenger $uid: ${e.message}")
            }
        }
        passengerMap = map
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
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
                        IconButton(onClick = {
                            selectedUser = driver
                            selectedUserRole = UserRole.DRIVER
                            isDialogOpen = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Driver details")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                        IconButton(onClick = {
                            selectedUser = passenger
                            selectedUserRole = UserRole.PASSENGER
                            isDialogOpen = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Passenger details")
                        }
                    }
                }
            }
        }

        if (isDialogOpen && selectedUser != null) {
            val context = LocalContext.current
            val stop = ride.pickupStops.firstOrNull { it.passengerId == selectedUser!!.uid }

            val (locationLabel, timeLabel, timeValue) = when (ride.direction) {
                RideDirection.TO_WORK -> Triple("Pickup Location", "Pickup Time", stop?.pickupTime ?: "Unknown")
                RideDirection.TO_HOME -> Triple("Dropoff Location", "Departure Time", stop?.dropoffTime ?: "Unknown")
                else -> Triple("Location", "Time", "Unknown")
            }

            val locationName = stop?.location?.name ?: "Unknown"

            AlertDialog(
                onDismissRequest = { isDialogOpen = false },
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = selectedUser!!.name)
                    }
                },
                text = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${selectedUser!!.email}")
                                    }
                                    context.startActivity(intent)
                                }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Email: ${selectedUser!!.email}", color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${selectedUser!!.phoneNumber}")
                                    }
                                    context.startActivity(intent)
                                }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Phone")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Phone: ${selectedUser!!.phoneNumber}", color = MaterialTheme.colorScheme.primary)
                        }

                        if (selectedUserRole == UserRole.PASSENGER) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$locationLabel: $locationName")
                            Text("$timeLabel: $timeValue")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { isDialogOpen = false }, modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Close")
                        }
                    }
                }
            )
        }
    }
}
