package com.wepool.app.ui.screens.rideHistory

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.SortOrder
import com.wepool.app.data.model.enums.ride.RideSortFields
import com.wepool.app.data.model.enums.ride.RideSortFieldsWithOrder
import com.wepool.app.data.model.enums.user.UserFilterFields
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.RoleOnlyFilters
import com.wepool.app.data.repository.interfaces.IRideRepository
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableCard
import com.wepool.app.ui.screens.rideHistory.RideHistoryCard
import kotlinx.coroutines.launch

@Composable
fun RideHistoryScreen(navController: NavController, uid: String) {
    val context = LocalContext.current
    val rideRepository: IRideRepository = RepositoryProvider.provideRideRepository()
    val coroutineScope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }

    var userRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var selectedFilters by remember { mutableStateOf<List<UserFilterFields>>(emptyList()) }
    var searchTriggered by remember { mutableStateOf(false) }
    var hasFilterBeenApplied by remember { mutableStateOf(false) }
    var rideSortFields by remember { mutableStateOf<List<RideSortFieldsWithOrder>>(emptyList()) }

    var filters by remember { mutableStateOf(RoleOnlyFilters()) }

    fun refreshRides() {
        coroutineScope.launch {
            try {
                loading = true
                error = null

                val baseRides = when {
                    UserFilterFields.USER_ROLE in selectedFilters && userRoleFilter == null -> {
                        Toast.makeText(
                            context,
                            "\u26A0 Please select a valid role to display ride history.",
                            Toast.LENGTH_SHORT
                        ).show()
                        loading = false
                        return@launch
                    }

                    UserFilterFields.USER_ROLE !in selectedFilters -> {
                        val driverRides = rideRepository.getPastRidesAsDriver(uid)
                        val passengerRides = rideRepository.getPastRidesAsPassenger(uid)
                        (driverRides + passengerRides).distinctBy { it.rideId }
                    }

                    userRoleFilter == UserRole.DRIVER -> rideRepository.getPastRidesAsDriver(uid)
                    userRoleFilter == UserRole.PASSENGER -> rideRepository.getPastRidesAsPassenger(uid)
                    userRoleFilter == UserRole.All -> {
                        val driverRides = rideRepository.getPastRidesAsDriver(uid)
                        val passengerRides = rideRepository.getPastRidesAsPassenger(uid)
                        (driverRides + passengerRides).distinctBy { it.rideId }
                    }

                    else -> emptyList()
                }

                rides = rideSortFields.fold(baseRides) { acc, sortField ->
                    when (sortField.field) {
                        RideSortFields.DATE ->
                            if (sortField.order == SortOrder.DESCENDING) acc.sortedByDescending { it.date }
                            else acc.sortedBy { it.date }

                        RideSortFields.DEPARTURE_TIME ->
                            if (sortField.order == SortOrder.DESCENDING) acc.sortedByDescending { it.departureTime }
                            else acc.sortedBy { it.departureTime }

                        RideSortFields.ARRIVAL_TIME ->
                            if (sortField.order == SortOrder.DESCENDING) acc.sortedByDescending { it.arrivalTime }
                            else acc.sortedBy { it.arrivalTime }

                        else -> acc
                    }
                }

                hasFilterBeenApplied = true
            } catch (e: Exception) {
                error = "\u274C Error loading rides: ${e.message}"
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
                        .padding(16.dp)
                        .padding(bottom = 64.dp)
                ) {
                    Text(
                        text = "Ride History",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ExpandableCard(
                        title = "Filter by Role",
                        filters = filters,
                        selectedSortFields = rideSortFields,
                        onSortFieldsChanged = {
                            rideSortFields = it.filterIsInstance<RideSortFieldsWithOrder>()
                        },
                        availableFilters = listOf(UserFilterFields.USER_ROLE),
                        selectedFilters = selectedFilters,
                        onFiltersChanged = {
                            filters = it as RoleOnlyFilters
                            userRoleFilter = filters.role
                        },
                        onSelectedFiltersChanged = { selectedFilters = it },
                        onSearchTriggeredChanged = {
                            searchTriggered = true
                            hasFilterBeenApplied = false
                        },
                        onSearchClicked = { refreshRides() },
                        showSort = true,
                        showFilter = true,
                        showCleanAllButton = true,
                        limitUserSuggestionsToCompany = false,
                        rideAvailableSortFields = listOf(
                            RideSortFields.DATE,
                            RideSortFields.DEPARTURE_TIME,
                            RideSortFields.ARRIVAL_TIME
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        !hasFilterBeenApplied -> Text(
                            "Please select a filter and press Refresh to show rides.",
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        rides.isEmpty() -> Text(
                            "No ride history found for selected role.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        else -> {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
}
