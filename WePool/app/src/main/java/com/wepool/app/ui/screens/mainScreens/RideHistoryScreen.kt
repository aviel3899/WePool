package com.wepool.app.ui.screens.mainScreens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email

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
    var filterExpanded by remember { mutableStateOf(true) }

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
                error = "❌ Error loading rides: ${e.message}"
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
                .padding(bottom = 64.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { filterExpanded = !filterExpanded },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = if (filterExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (filterExpanded) "Collapse Filter" else "Expand Filter"
                            )
                        }

                        Text(
                            text = "Filter by role",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    if (filterExpanded) {
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
                                        text = filterOptions.first { it.first == selectedFilter }.second,
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
                            Text("Search")
                        }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        items(rides) { ride ->
                            RideHistoryCard(ride = ride)
                            Spacer(modifier = Modifier.height(8.dp))
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
                rideId = null,
                navController = navController,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                showBackButton = true,
                showHomeButton = false
            )
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
                    val context = LocalContext.current

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${selectedPassenger!!.email}")
                                }
                                context.startActivity(intent)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email: ${selectedPassenger!!.email}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${selectedPassenger!!.phoneNumber}")
                                }
                                context.startActivity(intent)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phone: ${selectedPassenger!!.phoneNumber}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val pickupStop =
                        ride.pickupStops.firstOrNull { it.passengerId == selectedPassenger!!.uid }
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
                    val context = LocalContext.current

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${selectedDriverUser!!.email}")
                                }
                                context.startActivity(intent)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email: ${selectedDriverUser!!.email}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${selectedDriverUser!!.phoneNumber}")
                                }
                                context.startActivity(intent)
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phone: ${selectedDriverUser!!.phoneNumber}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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



