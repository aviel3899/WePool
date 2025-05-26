package com.wepool.app.ui.screens.passengerScreens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.logic.PassengerRideFinder
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.FavoriteLocationDropdown
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerRideSearchScreen(
    navController: NavController,
    uid: String,
    direction: RideDirection
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isToHome = direction == RideDirection.TO_HOME
    var fixedLocation by remember { mutableStateOf<LocationData?>(null) }
    val title = if (isToHome) "Join a Homebound Ride" else "Join a Workbound Ride"

    var companyId by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf(LocationData()) }
    var locationSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var passengerNotes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var rides by remember { mutableStateOf<List<RideCandidate>>(emptyList()) }
    var ridesFetched by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(true) }

    var pickupStop by remember { mutableStateOf(PickupStop()) }
    val mapsService = RepositoryProvider.mapsService
    val rideRepository = RepositoryProvider.provideRideRepository()
    val rideRequestRepository = RepositoryProvider.provideRideRequestRepository()
    val userRepository = RepositoryProvider.provideUserRepository()
    val passengerRideFinder = PassengerRideFinder(mapsService, RouteMatcher)

    var favoriteLocations by remember { mutableStateOf<List<LocationData>>(emptyList()) }

    LaunchedEffect(uid) {
        coroutineScope.launch {
            val user = userRepository.getUser(uid)
            favoriteLocations = user?.favoriteLocations ?: emptyList()
            val companyCode = user?.companyCode.orEmpty()

            val company =
                RepositoryProvider.provideCompanyRepository().getCompanyByCode(companyCode)

            if (company != null) {
                companyId = company.companyId
                val location = RepositoryProvider.provideCompanyRepository()
                    .getLocationByCompanyCode(companyId)
                fixedLocation = location
            } else {

                Log.e("CompanyRepository", "❌ חברה לא נמצאה עבור קוד החברה: $companyCode")
            }
        }
    }


    LaunchedEffect(locationInput.name, selectedDate, selectedTime) {
        isFormValid =
            locationInput.name.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (fixedLocation != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { showDetails = !showDetails }) {
                            Icon(
                                imageVector = if (showDetails) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    }

                    if (showDetails) {
                        Spacer(modifier = Modifier.height(24.dp))

                        if (isToHome) {
                            OutlinedTextField(
                                value = fixedLocation!!.name,
                                onValueChange = {},
                                label = { Text("Start Location") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        FavoriteLocationDropdown(
                            label = if (isToHome) "Destination" else "Start Location",
                            locationData = locationInput,
                            onLocationSelected = { locationInput = it },
                            onTextChanged = { text ->
                                locationInput = locationInput.copy(name = text)
                                coroutineScope.launch {
                                    locationSuggestions = mapsService.getAddressSuggestions(text)
                                }
                            },
                            suggestions = locationSuggestions,
                            favoriteLocations = favoriteLocations,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!isToHome) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = fixedLocation!!.name,
                                onValueChange = {},
                                label = { Text("Destination") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passengerNotes,
                            onValueChange = { passengerNotes = it },
                            label = { Text("Notes for Driver (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            singleLine = false,
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                    calendar.set(year, month, day)
                                    selectedDate = sdf.format(calendar.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                datePicker.minDate = calendar.timeInMillis
                                show()
                            }
                        }) {
                            Text(if (selectedDate.isNotBlank()) "Selected Date: $selectedDate" else "Pick a Date")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.HOUR_OF_DAY, hour)
                                    cal.set(Calendar.MINUTE, minute)
                                    val todayStr = SimpleDateFormat(
                                        "dd-MM-yyyy",
                                        Locale.getDefault()
                                    ).format(now.time)
                                    if (selectedDate == todayStr && cal.before(now)) {
                                        Toast.makeText(
                                            context,
                                            "❌ Please select a future time.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        selectedTime = String.format("%02d:%02d", hour, minute)
                                    }
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                true
                            ).show()
                        }) {
                            Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else if (isToHome) "Pick a Departure Time" else "Pick an Arrival Time")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val geo = mapsService.getCoordinatesFromAddress(locationInput.name)
                                if (geo == null) {
                                    Toast.makeText(
                                        context,
                                        "❌ Address not found",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    return@launch
                                }
                                pickupStop = PickupStop(location = geo, passengerId = uid)
                                val availableRides =
                                    passengerRideFinder.getAvailableRidesForPassenger(
                                        companyId = companyId,
                                        direction = direction,
                                        passengerArrivalTime = if (!isToHome) selectedTime else "",
                                        passengerDepartureTime = if (isToHome) selectedTime else "",
                                        passengerDate = selectedDate,
                                        pickupPoint = pickupStop,
                                        rideRepository = rideRepository,
                                        rideRequestRepository = rideRequestRepository
                                    )
                                rides = availableRides
                                ridesFetched = true
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "❌ Error fetching rides",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    }, enabled = isFormValid, modifier = Modifier.fillMaxWidth()) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching...")
                        } else {
                            Text("Show Available Rides")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    if (ridesFetched) {
                        if (rides.isEmpty()) {
                            Text(
                                "❌ No available rides matching your request.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(rides) { ride ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Start: ${ride.ride.startLocation.name}")
                                            Text("Destination: ${ride.ride.destination.name}")
                                            Text("Date: ${ride.ride.date}")
                                            Text("Arrival Time: ${ride.detourEvaluationResult.updatedReferenceTime}")
                                            val timeLabel =
                                                if (direction == RideDirection.TO_WORK) {
                                                    ride.detourEvaluationResult.pickupLocation?.pickupTime
                                                        ?: "לא ידוע"
                                                } else {
                                                    ride.detourEvaluationResult.pickupLocation?.dropoffTime
                                                        ?: "לא ידוע"
                                                }
                                            val timeLabelTitle =
                                                if (direction == RideDirection.TO_WORK) "Pickup Time" else "Dropoff Time"
                                            Text("$timeLabelTitle: $timeLabel")

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(onClick = {
                                                coroutineScope.launch {
                                                    val success = rideRequestRepository.sendRequest(
                                                        rideId = ride.ride.rideId,
                                                        passengerId = uid,
                                                        pickupLocation = pickupStop.location,
                                                        detourEvaluationResult = ride.detourEvaluationResult,
                                                        notes = passengerNotes
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        if (success) "✅ Request sent" else "❌ Failed to send request",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    if (success) {
                                                        rides =
                                                            rides.filterNot { it.ride.rideId == ride.ride.rideId }
                                                    }
                                                }
                                            }, modifier = Modifier.fillMaxWidth()) {
                                                Text("Send Request")
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
                        rideId = null,
                        navController = navController,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        showBackButton = true,
                        showHomeButton = true
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}