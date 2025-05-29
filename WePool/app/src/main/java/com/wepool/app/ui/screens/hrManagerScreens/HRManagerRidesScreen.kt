package com.wepool.app.ui.screens.hrManagerScreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.FilterFields
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableSortCard
import com.wepool.app.ui.screens.components.RideDataProvider
import com.wepool.app.ui.screens.components.RideMapDialog
import com.wepool.app.ui.screens.components.sortFields.RideSortManager
import com.wepool.app.ui.screens.adminScreens.rides.RideCard
import kotlinx.coroutines.launch

@Composable
fun HRManagerRidesScreen(
    uid: String,
    navController: NavController,
    filterByUid: Boolean = false
) {
    val rideRepository = RepositoryProvider.provideRideRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val companyRepository = RepositoryProvider.provideCompanyRepository()
    val coroutineScope = rememberCoroutineScope()

    val initialFilters = remember(uid, filterByUid) {
        if (filterByUid) {
            RideSearchFilters(userNameOrEmail = "Loading user...")
        } else {
            RideSearchFilters()
        }
    }

    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var allRides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var allFetchedRides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var filteredRides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var rideForMapDialog by remember { mutableStateOf<Ride?>(null) }

    var filters by remember { mutableStateOf(initialFilters) }
    var selectedFilters by remember {
        mutableStateOf(
            if (filterByUid) listOf(FilterFields.USER_NAME)
            else emptyList()
        )
    }
    var searchTriggered by remember { mutableStateOf(filterByUid) }

    val availableFilters = listOf(FilterFields.USER_NAME, FilterFields.DATE_RANGE, FilterFields.TIME_RANGE, FilterFields.DIRECTION)

    var companyCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val currentUser = userRepository.getUser(uid)
            companyCode = currentUser?.companyCode ?: ""
            val company = companyRepository.getCompanyByCode(companyCode)

            val users = userRepository.getAllUsers()
            allUsers = users.filter { it.companyCode == companyCode }

            val rides = rideRepository.getAllRides()
            allFetchedRides = rides
            allRides = rides.filter { it.companyCode == companyCode }

            if (filterByUid) {
                val matchedUser = allUsers.find { it.uid == uid }
                matchedUser?.let { user ->
                    filters = RideSearchFilters(userNameOrEmail = "${user.name} (${user.email})")
                }
                selectedFilters = listOf(FilterFields.USER_NAME)

                val filtered = allRides.filter { ride ->
                    val driverMatch = ride.driverId == uid
                    val passengerMatch = ride.pickupStops.any { it.passengerId == uid }
                    driverMatch || passengerMatch
                }
                filteredRides = RideSortManager.sortRides(
                    filtered,
                    filters.sortFields,
                    users.associateBy { it.uid },
                    emptyMap()
                )
            }

            if (!filterByUid) {
                filteredRides = emptyList()
            }
        }
    }

    RideDataProvider { userMap, _ ->
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
                    filters = filters,
                    onFiltersChanged = { updated -> filters = updated },
                    onSearchClicked = {
                        searchTriggered = true

                        val filtered = allRides.filter { ride ->
                            val matchesUser = if (FilterFields.USER_NAME !in selectedFilters) {
                                true
                            } else {
                                filters.userNameOrEmail?.let { query ->
                                    val driverMatch = userMap[ride.driverId]?.let { user ->
                                        val full = "${user.name} (${user.email})"
                                        user.name.contains(query, true) ||
                                                user.email.contains(query, true) ||
                                                full.contains(query, true)
                                    } ?: false
                                    val passengerMatch = ride.pickupStops.any { stop ->
                                        userMap[stop.passengerId]?.let { user ->
                                            val full = "${user.name} (${user.email})"
                                            user.name.contains(query, true) ||
                                                    user.email.contains(query, true) ||
                                                    full.contains(query, true)
                                        } ?: false
                                    }
                                    driverMatch || passengerMatch
                                } ?: true
                            }

                            val matchesDirection = filters.direction?.let { it == ride.direction } ?: true
                            val matchesDateStart = filters.dateFrom?.takeIf { it.isNotBlank() }?.let { ride.date >= it } ?: true
                            val matchesDateEnd = filters.dateTo?.takeIf { it.isNotBlank() }?.let { ride.date <= it } ?: true
                            val matchesTimeStart = filters.timeFrom?.takeIf { it.isNotBlank() }?.let { ride.departureTime ?: "" >= it } ?: true
                            val matchesTimeEnd = filters.timeTo?.takeIf { it.isNotBlank() }?.let { ride.departureTime ?: "" <= it } ?: true

                            matchesUser && matchesDirection &&
                                    matchesDateStart && matchesDateEnd &&
                                    matchesTimeStart && matchesTimeEnd
                        }

                        filteredRides = RideSortManager.sortRides(
                            filtered,
                            filters.sortFields,
                            userMap,
                            emptyMap()
                        )
                    },
                    showSort = true,
                    showFilter = true,
                    showCleanAllButton = true,
                    usersInCompany = allUsers,
                    limitUserSuggestionsToCompany = true,
                    onSearchTriggeredChanged = { searchTriggered = false },
                    onCleanAllClicked = {
                        filters = RideSearchFilters()
                        selectedFilters = emptyList()
                        searchTriggered = true
                        allRides = allFetchedRides.filter { it.companyCode == companyCode }
                        filteredRides = emptyList()

                        coroutineScope.launch {
                            searchTriggered = false
                            kotlinx.coroutines.delay(50)
                            searchTriggered = true
                        }
                    }
                )

                if (searchTriggered) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredRides) { ride ->
                            RideCard(
                                ride = ride,
                                selectedUserUid = if (filterByUid) uid else null,
                                onShowMapClicked = { rideForMapDialog = it }
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

            rideForMapDialog?.let { ride ->
                RideMapDialog(ride = ride, onDismiss = { rideForMapDialog = null })
            }
        }
    }
}
