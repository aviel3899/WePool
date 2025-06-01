package com.wepool.app.ui.screens.adminScreens.rides

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.ride.RideFilterFields
import com.wepool.app.data.model.enums.ride.RideSortFieldsWithOrder
import com.wepool.app.data.model.enums.ride.RideSortFields
import com.wepool.app.data.model.enums.user.UserSortFieldWithOrder
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.ride.RideSearchFilters
import com.wepool.app.data.model.users.User
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.ExpandableCard
import com.wepool.app.ui.screens.components.RideDataProvider
import com.wepool.app.ui.screens.components.sortFields.ride.RideSortManager
import kotlinx.coroutines.launch
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.RideMapDialog

@Composable
fun RidesListScreen(
    uid: String,
    navController: NavController,
    filterByUid: Boolean = false
) {
    val rideRepository = RepositoryProvider.provideRideRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
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
    var filteredRides by remember { mutableStateOf<List<Ride>>(emptyList()) }

    var filters by remember { mutableStateOf(initialFilters) }
    var selectedFilters by remember {
        mutableStateOf(
            if (filterByUid) listOf(RideFilterFields.USER_NAME)
            else emptyList()
        )
    }

    var searchTriggered by remember { mutableStateOf(filterByUid) }

    val availableFilters = RideFilterFields.values().toList()

    var rideForMapDialog by remember { mutableStateOf<Ride?>(null) }

    var allFetchedRides by remember { mutableStateOf<List<Ride>>(emptyList()) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val users = userRepository.getAllUsers()
            allUsers = users

            val rides = rideRepository.getAllRides()
            allFetchedRides = rides

            allRides = if (filterByUid) {
                rides.filter {
                    it.driverId == uid || it.pickupStops.any { stop -> stop.passengerId == uid }
                }
            } else rides

            if (filterByUid) {
                val matchedUser = users.find { it.uid == uid }
                matchedUser?.let { user ->
                    filters = RideSearchFilters(userNameOrEmail = "${user.name} (${user.email})")
                }
                selectedFilters = listOf(RideFilterFields.USER_NAME)
            }

            filteredRides = emptyList()
        }
    }

    RideDataProvider { userMap, companyNameMap ->

        LaunchedEffect(filterByUid, allRides, filters) {
            if (
                filterByUid &&
                allRides.isNotEmpty() &&
                filters.userNameOrEmail?.isNotBlank() == true
            ) {
                val filtered = allRides.filter { ride ->
                    ride.driverId == uid || ride.pickupStops.any { it.passengerId == uid }
                }
                filteredRides = RideSortManager.sortRides(
                    filtered,
                    filters.sortFields,
                    userMap,
                    companyNameMap
                )
            }
        }

        BackgroundWrapper {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 96.dp)
                    ) {
                        ExpandableCard(
                            selectedSortFields = filters.sortFields,
                            onSortFieldsChanged = {
                                filters =
                                    filters.copy(sortFields = it.filterIsInstance<RideSortFieldsWithOrder>())
                            },
                            availableFilters = availableFilters,
                            selectedFilters = selectedFilters,
                            onSelectedFiltersChanged = {
                                selectedFilters = it.filterIsInstance<RideFilterFields>()
                            },
                            filters = filters,
                            onFiltersChanged = { updated ->
                                if (updated is RideSearchFilters) {
                                    filters = updated
                                }
                            },
                            rideAvailableSortFields = RideSortFields.values().toList(),
                            onSearchClicked = {
                                searchTriggered = true

                                val filtered = allRides.filter { ride ->
                                    val matchesCompany =
                                        filters.companyName?.let { it == companyNameMap[ride.companyCode] }
                                            ?: true

                                    val matchesUser =
                                        if (RideFilterFields.USER_NAME !in selectedFilters) {
                                            true
                                        } else {
                                            filters.userNameOrEmail?.let { query ->
                                                val driverMatch =
                                                    userMap[ride.driverId]?.let { user ->
                                                        user.name.contains(
                                                            query,
                                                            ignoreCase = true
                                                        ) ||
                                                                user.email.contains(
                                                                    query,
                                                                    ignoreCase = true
                                                                )
                                                    } ?: false
                                                val passengerMatch = ride.pickupStops.any { stop ->
                                                    userMap[stop.passengerId]?.let { user ->
                                                        user.name.contains(
                                                            query,
                                                            ignoreCase = true
                                                        ) ||
                                                                user.email.contains(
                                                                    query,
                                                                    ignoreCase = true
                                                                )
                                                    } ?: false
                                                }
                                                driverMatch || passengerMatch
                                            } ?: true
                                        }

                                    val matchesDirection =
                                        filters.direction?.let { it == ride.direction } ?: true
                                    val matchesDateStart =
                                        filters.dateFrom?.takeIf { it.isNotBlank() }
                                            ?.let { ride.date >= it } ?: true
                                    val matchesDateEnd = filters.dateTo?.takeIf { it.isNotBlank() }
                                        ?.let { ride.date <= it } ?: true
                                    val matchesTimeStart =
                                        filters.timeFrom?.takeIf { it.isNotBlank() }
                                            ?.let { ride.departureTime ?: "" >= it } ?: true
                                    val matchesTimeEnd = filters.timeTo?.takeIf { it.isNotBlank() }
                                        ?.let { ride.departureTime ?: "" <= it } ?: true

                                    matchesCompany && matchesUser && matchesDirection &&
                                            matchesDateStart && matchesDateEnd &&
                                            matchesTimeStart && matchesTimeEnd
                                }

                                filteredRides = RideSortManager.sortRides(
                                    filtered,
                                    filters.sortFields,
                                    userMap,
                                    companyNameMap
                                )
                            },
                            showSort = true,
                            showFilter = true,
                            showCleanAllButton = true,
                            usersInCompany = allUsers,
                            limitUserSuggestionsToCompany = false,
                            onSearchTriggeredChanged = { searchTriggered = false },
                            onCleanAllClicked = {
                                filters = RideSearchFilters()
                                selectedFilters = emptyList()
                                searchTriggered = true
                                allRides = allFetchedRides
                                filteredRides = emptyList()

                                coroutineScope.launch {
                                    searchTriggered = false
                                    kotlinx.coroutines.delay(50)
                                    searchTriggered = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            when {
                                !searchTriggered -> Text(
                                    "Please enter filters and press Search.",
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                filteredRides.isEmpty() -> Text(
                                    "No rides found.",
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredRides) { ride ->
                                        RideCard(
                                            ride = ride,
                                            selectedUserUid = if (filterByUid) uid else null,
                                            onShowMapClicked = { rideForMapDialog = ride }
                                        )
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
                            showHomeButton = true
                        )
                    }

                    rideForMapDialog?.let { ride ->
                        RideMapDialog(ride = ride, onDismiss = { rideForMapDialog = null })
                    }
                }
            }
        }
    }
}