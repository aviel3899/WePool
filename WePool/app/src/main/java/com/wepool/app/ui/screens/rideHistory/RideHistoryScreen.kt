package com.wepool.app.ui.screens.rideHistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.wepool.app.data.model.enums.UserRole
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                    navController = navController,
                    showBackButton = true,
                    showHomeButton = false
                )
            }
        }
    }
}

