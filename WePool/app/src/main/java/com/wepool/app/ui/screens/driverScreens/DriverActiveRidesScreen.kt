package com.wepool.app.ui.screens.driverScreens

import android.Manifest
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wepool.app.data.model.enums.ride.*
import com.wepool.app.data.model.ride.*
import com.wepool.app.data.model.users.User
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.infrastructure.navigation.RideNavigationServiceController
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.adminScreens.rides.RideCard
import com.wepool.app.ui.screens.components.*
import com.wepool.app.ui.screens.components.sortFields.ride.RideSortManager
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DriverActiveRidesScreen(uid: String, navController: NavController, rideId: String? = null) {
    val context = LocalContext.current
    val driverRepository = RepositoryProvider.provideDriverRepository()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val requestRepository = RepositoryProvider.provideRideRequestRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var rides by remember { mutableStateOf<List<Ride>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isActionInProgress by remember { mutableStateOf(false) }
    var selectedRide by remember { mutableStateOf<Ride?>(null) }
    var selectedPassengerUser by remember { mutableStateOf<User?>(null) }
    var passengerNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedPassengerStop by remember { mutableStateOf<PickupStop?>(null) }
    var showTooLateDialog by remember { mutableStateOf(false) }
    var innerTooLateDialog by remember { mutableStateOf(false) }
    var threshold by remember { mutableStateOf(0) }
    var rideForMapDialog by remember { mutableStateOf<Ride?>(null) }

    val availableFilters = listOf(
        RideFilterFields.DATE_RANGE,
        RideFilterFields.TIME_RANGE,
        RideFilterFields.DIRECTION
    )
    val availableSortFields = listOf(
        RideSortFields.DATE,
        RideSortFields.DEPARTURE_TIME,
        RideSortFields.ARRIVAL_TIME
    )

    var filters by remember { mutableStateOf(RideSearchFilters(sortFields = emptyList())) }
    var selectedFilters by remember { mutableStateOf(emptyList<RideFilterFields>()) }
    var selectedSortFields by remember { mutableStateOf<List<RideSortFieldsWithOrder>>(emptyList()) }

    val userMap by produceState<Map<String, User>>(initialValue = emptyMap()) {
        value = RepositoryProvider.provideUserRepository().getAllUsers().associateBy { it.uid }
    }
    val companyNameMap by produceState<Map<String, String>>(initialValue = emptyMap()) {
        value = RepositoryProvider.provideCompanyRepository().getAllCompanies()
            .associateBy({ it.companyCode }, { it.companyName })
    }

    fun refreshRides() {
        coroutineScope.launch {
            try {
                loading = true
                error = null
                val allRides = driverRepository.getActiveRidesForDriver(uid)
                val filtered = allRides.filter {
                    val matchesDirection =
                        filters.direction?.let { dir -> it.direction == dir } ?: true
                    val matchesDateStart = filters.dateFrom?.let { df -> it.date >= df } ?: true
                    val matchesDateEnd = filters.dateTo?.let { dt -> it.date <= dt } ?: true
                    val matchesTimeStart =
                        filters.timeFrom?.let { tf -> (it.departureTime ?: "") >= tf } ?: true
                    val matchesTimeEnd =
                        filters.timeTo?.let { tt -> (it.departureTime ?: "") <= tt } ?: true
                    matchesDirection && matchesDateStart && matchesDateEnd && matchesTimeStart && matchesTimeEnd
                }
                rides =
                    RideSortManager.sortRides(filtered, filters.sortFields, userMap, companyNameMap)
            } catch (e: Exception) {
                error = "❌Error loading Rides: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    val calendar = remember { Calendar.getInstance() }

    DriverActiveRidesContent(
        uid = uid,
        rides = rides,
        rideId = rideId,
        loading = loading,
        error = error,
        navController = navController,
        filters = filters,
        selectedFilters = selectedFilters,
        selectedSortFields = selectedSortFields,
        availableFilters = availableFilters,
        availableSortFields = availableSortFields,
        onFiltersChanged = { filters = it },
        onSelectedFiltersChanged = { selectedFilters = it },
        onSortFieldsChanged = {
            selectedSortFields = it
            filters = filters.copy(sortFields = it)
        },
        onRefreshClicked = { refreshRides() },
        onStartRideClicked = { ride ->
            coroutineScope.launch {
                if (isActionInProgress) return@launch
                isActionInProgress = true
                if (!locationPermissionState.status.isGranted) {
                    locationPermissionState.launchPermissionRequest()
                    error = "📍 need to approve location access"
                    isActionInProgress = false
                    return@launch
                }
                try {
                    RideNavigationServiceController.startRideNavigation(context, ride.rideId)
                } catch (e: Exception) {
                    error = "⚠ ${e.message}"
                } finally {
                    isActionInProgress = false
                }
            }
        },
        onCancelRideClicked = { ride ->
            coroutineScope.launch {
                if (isActionInProgress) return@launch
                isActionInProgress = true
                try {
                    val rideDateParts = ride.date.split("-").map { it.toInt() }
                    val rideTimeParts = ride.departureTime!!.split(":").map { it.toInt() }
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
                    val diffMillis = rideCalendar.timeInMillis - now.timeInMillis
                    val diffMinutes = diffMillis / (60 * 1000)
                    val noPassengers = ride.pickupStops.isEmpty()
                    threshold = if (ride.direction == RideDirection.TO_WORK) 60 else 10
                    if (!noPassengers && diffMinutes < threshold) {
                        showTooLateDialog = true
                        return@launch
                    }
                    rideRepository.deleteRide(ride.rideId)
                    refreshRides()
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    isActionInProgress = false
                }
            }
        },
        onShowMapClicked = { rideForMapDialog = it },
        onPassengerDetailsClicked = { ride ->
            selectedRide = ride
            coroutineScope.launch {
                val names = mutableMapOf<String, String>()
                ride.pickupStops.forEach { stop ->
                    userRepository.getUser(stop.passengerId)?.let { user ->
                        names[stop.passengerId] = user.name
                    }
                }
                passengerNames = names
                showDetailsDialog = true
            }
        },
        onCleanAllClicked = {
            filters = RideSearchFilters(sortFields = emptyList())
            selectedFilters = emptyList()
            selectedSortFields = emptyList()
            rides = emptyList()
            coroutineScope.launch {
                kotlinx.coroutines.delay(50)
            }
        }
    )

    rideForMapDialog?.let { RideMapDialog(ride = it, onDismiss = { rideForMapDialog = null }) }

    RidePassengerDetailsDialog(
        showDialog = showDetailsDialog,
        ride = selectedRide,
        passengerNames = passengerNames,
        userRepository = userRepository,
        rideRepository = rideRepository,
        requestRepository = requestRepository,
        onDismiss = {
            showDetailsDialog = false
            selectedPassengerStop = null
            selectedPassengerUser = null
        },
        onPassengerRemoved = {
            refreshRides()
            showDetailsDialog = false
        }
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (showTooLateDialog || innerTooLateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTooLateDialog = false
                    innerTooLateDialog = false
                },
                title = { Text("Too Late") },
                text = {
                    Text("This action is not allowed. You cannot cancel a ride less than $threshold minutes before departure time.")
                },
                confirmButton = {
                    Button(onClick = {
                        showTooLateDialog = false
                        innerTooLateDialog = false
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun DriverActiveRidesContent(
    uid: String,
    rides: List<Ride>,
    rideId: String?,
    loading: Boolean,
    error: String?,
    navController: NavController,
    filters: RideSearchFilters,
    selectedFilters: List<RideFilterFields>,
    selectedSortFields: List<RideSortFieldsWithOrder>,
    availableFilters: List<RideFilterFields>,
    availableSortFields: List<RideSortFields>,
    onFiltersChanged: (RideSearchFilters) -> Unit,
    onSelectedFiltersChanged: (List<RideFilterFields>) -> Unit,
    onSortFieldsChanged: (List<RideSortFieldsWithOrder>) -> Unit,
    onRefreshClicked: () -> Unit,
    onStartRideClicked: (Ride) -> Unit,
    onCancelRideClicked: (Ride) -> Unit,
    onShowMapClicked: (Ride) -> Unit,
    onPassengerDetailsClicked: (Ride) -> Unit,
    onCleanAllClicked: () -> Unit
) {
    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 96.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Your Active Rides",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ExpandableCard(
                        filters = filters,
                        selectedSortFields = selectedSortFields,
                        onSortFieldsChanged = { onSortFieldsChanged(it.filterIsInstance<RideSortFieldsWithOrder>()) },
                        availableFilters = availableFilters,
                        selectedFilters = selectedFilters,
                        onSelectedFiltersChanged = onSelectedFiltersChanged,
                        onFiltersChanged = { updated ->
                            if (updated is RideSearchFilters) {
                                onFiltersChanged(updated)
                            }
                        },
                        rideAvailableSortFields = availableSortFields,
                        onSearchClicked = onRefreshClicked,
                        onCleanAllClicked = onCleanAllClicked,
                        showCompanyName = false,
                        showUserName = false,
                        showDate = true,
                        showDepartureTime = true,
                        showArrivalTime = true,
                        showFilter = true,
                        showSort = true,
                        showCleanAllButton = true,
                        title = "Filter Rides"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    when {
                        loading -> CircularProgressIndicator()
                        error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                        rides.isEmpty() -> Text("Click 'Apply Filter' to search for active rides.")
                        else -> {
                            val filteredRides =
                                if (!rideId.isNullOrEmpty()) rides.filter { it.rideId == rideId } else rides

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredRides) { ride ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("From: ${ride.startLocation.name}")
                                            Text("To: ${ride.destination.name}")
                                            Text("Date: ${ride.date}")
                                            Text("Departure Time: ${ride.departureTime}")
                                            Text("Arrival Time: ${ride.arrivalTime}")
                                            Text("Stops on the way: ${ride.pickupStops.size}")
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                listOf(
                                                    Triple(
                                                        "Start",
                                                        Icons.Default.PlayArrow,
                                                        Color(0xFF2E7D32)
                                                    ) to { onStartRideClicked(ride) },
                                                    Triple(
                                                        "Cancel",
                                                        Icons.Default.Cancel,
                                                        Color(0xFFC62828)
                                                    ) to { onCancelRideClicked(ride) },
                                                    Triple(
                                                        "Map",
                                                        Icons.Default.Map,
                                                        Color(0xFF039BE5)
                                                    ) to { onShowMapClicked(ride) },
                                                    Triple(
                                                        "Passengers",
                                                        Icons.Default.Person,
                                                        Color(0xFFFBC02D)
                                                    ) to { onPassengerDetailsClicked(ride) }
                                                ).chunked(2).forEach { rowButtons ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            16.dp,
                                                            Alignment.CenterHorizontally
                                                        )
                                                    ) {
                                                        rowButtons.forEach { (data, action) ->
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                OutlinedButton(
                                                                    onClick = action,
                                                                    modifier = Modifier
                                                                        .width(160.dp)
                                                                        .height(100.dp),
                                                                    shape = MaterialTheme.shapes.large,
                                                                    border = ButtonDefaults.outlinedButtonBorder(
                                                                        enabled = true
                                                                    ),
                                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                                        contentColor = data.third
                                                                    )
                                                                ) {
                                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                        Icon(
                                                                            imageVector = data.second,
                                                                            contentDescription = data.first,
                                                                            tint = data.third,
                                                                            modifier = Modifier.size(
                                                                                48.dp
                                                                            )
                                                                        )
                                                                        Spacer(
                                                                            modifier = Modifier.height(
                                                                                4.dp
                                                                            )
                                                                        )
                                                                        Text(
                                                                            data.first,
                                                                            style = MaterialTheme.typography.labelSmall
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
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
            }
        }
    }
}