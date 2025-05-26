package com.wepool.app.ui.screens.adminScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.UserSearchAutoComplete
import com.wepool.app.ui.screens.adminScreens.RideCard
import kotlinx.coroutines.launch

@Composable
fun RidesListScreen(uid: String, navController: NavController) {
    val userRepository = RepositoryProvider.provideUserRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var filteredRides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var selectedUserUid by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filterExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                loading = true
                users = userRepository.getAllUsers()
                rides = rideRepository.getAllRides()
                filteredRides = emptyList()
            } catch (e: Exception) {
                error = "❌ Failed to load rides: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    fun applyFilter() {
        filteredRides = if (selectedUserUid.isNullOrEmpty()) {
            rides // כל הנסיעות
        } else {
            rides.filter { ride ->
                ride.driverId == selectedUserUid || ride.pickupStops.any { it.passengerId == selectedUserUid }
            }
        }
    }

    fun clearFilter() {
        selectedUserUid = null
        filteredRides = emptyList()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
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
                                    contentDescription = null
                                )
                            }

                            Text(
                                text = "Search Rides by User",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        if (filterExpanded) {
                            Spacer(modifier = Modifier.height(16.dp))

                            UserSearchAutoComplete(
                                onUserSelected = { uid, _ -> selectedUserUid = uid },
                                onClear = { clearFilter() },
                                usersProvider = {
                                    try {
                                        users.map { user -> user.uid to "${user.name} (${user.email})" }
                                    } catch (e: Exception) {
                                        emptyList()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { applyFilter() },
                                modifier = Modifier.widthIn(min = 100.dp)
                            ) {
                                Text("Search")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    when {
                        loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

                        error != null -> {
                            Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        filteredRides.isEmpty() -> {
                            Text(
                                "No rides found.",
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredRides) { ride ->
                                    RideCard(
                                        ride = ride,
                                        selectedUserUid = if (selectedUserUid.isNullOrEmpty()) null else selectedUserUid
                                    )
                                }
                            }
                        }
                    }
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
}
