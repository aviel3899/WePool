package com.wepool.app.ui.screens.adminScreens.rides

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.FilterField
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableSortCard
import com.wepool.app.ui.screens.adminScreens.rides.RideCard
import com.wepool.app.ui.screens.components.RideDataProvider
import com.wepool.app.ui.screens.components.sortFields.RideSortManager
import kotlinx.coroutines.launch
import com.wepool.app.infrastructure.RepositoryProvider

@Composable
fun RidesListScreen(
    uid: String,
    navController: NavController,
    filterByUid: Boolean = false
) {
    val rideRepository = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    var allRides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var filteredRides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var filters by remember { mutableStateOf(RideSearchFilters()) }
    var selectedFilters by remember { mutableStateOf<List<FilterField>>(emptyList()) }
    var searchTriggered by remember { mutableStateOf(false) }

    val availableFilters = FilterField.values().toList()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val rides = rideRepository.getAllRides()
            allRides = if (filterByUid) {
                rides.filter {
                    it.driverId == uid || it.pickupStops.any { stop -> stop.passengerId == uid }
                }
            } else rides
            filteredRides = emptyList()
        }
    }

    RideDataProvider { userMap, companyNameMap ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp)
            ) {
                ExpandableSortCard(
                    selectedSortFields = filters.sortFields,
                    onSortFieldsChanged = {
                        filters = filters.copy(sortFields = it)
                    },
                    availableFilters = availableFilters,
                    selectedFilters = selectedFilters,
                    onSelectedFiltersChanged = { selectedFilters = it },
                    onFiltersChanged = { updated -> filters = updated },
                    onSearchClicked = {
                        searchTriggered = true
                        val filtered = allRides.filter { ride ->
                            val matchesCompany = filters.companyName?.let { it == companyNameMap[ride.companyCode] } ?: true
                            val matchesUser = filters.userNameOrEmail?.let { query ->
                                val driverMatch = userMap[ride.driverId]?.let { user ->
                                    user.name.contains(query, ignoreCase = true) || user.email.contains(query, ignoreCase = true)
                                } ?: false
                                val passengerMatch = ride.pickupStops.any { stop ->
                                    userMap[stop.passengerId]?.let { user ->
                                        user.name.contains(query, ignoreCase = true) || user.email.contains(query, ignoreCase = true)
                                    } ?: false
                                }
                                driverMatch || passengerMatch
                            } ?: true
                            val matchesDirection = filters.direction?.let { it == ride.direction } ?: true
                            val matchesDateStart = filters.dateFrom?.takeIf { it.isNotBlank() }?.let { ride.date >= it } ?: true
                            val matchesDateEnd = filters.dateTo?.takeIf { it.isNotBlank() }?.let { ride.date <= it } ?: true
                            val matchesTimeStart = filters.timeFrom?.takeIf { it.isNotBlank() }?.let { ride.departureTime ?: "" >= it } ?: true
                            val matchesTimeEnd = filters.timeTo?.takeIf { it.isNotBlank() }?.let { ride.departureTime ?: "" <= it } ?: true

                            matchesCompany && matchesUser && matchesDirection &&
                                    matchesDateStart && matchesDateEnd && matchesTimeStart && matchesTimeEnd
                        }

                        filteredRides = RideSortManager.sortRides(
                            filtered,
                            filters.sortFields,
                            userMap,
                            companyNameMap
                        )
                    }
                )

                if (searchTriggered) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredRides) { ride ->
                            RideCard(
                                ride = ride,
                                selectedUserUid = if (filterByUid) uid else null,
                                onShowMapClicked = { /* Future logic */ }
                            )
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
        }
    }
}
