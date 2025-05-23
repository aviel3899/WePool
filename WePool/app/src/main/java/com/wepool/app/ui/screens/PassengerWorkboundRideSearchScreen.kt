package com.wepool.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.data.model.logic.PassengerRideFinder
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PassengerWorkboundRideSearchScreen(navController: NavController, uid: String) {
    val context = LocalContext.current
    val destination = "מכללת אפקה"
    var startLocation by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var passengerNotes by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }
    var startLocationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    var showDetails by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val rideRequestRepository = RepositoryProvider.provideRideRequestRepository()
    val passengerRideFinder = PassengerRideFinder(
        mapsService = RepositoryProvider.mapsService,
        routeMatcher = RouteMatcher
    )
    var rides by remember { mutableStateOf<List<RideCandidate>>(emptyList()) }
    var ridesFetched by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    var pickupStop by remember { mutableStateOf(PickupStop()) }

    LaunchedEffect(startLocation, selectedDate, selectedTime) {
        isFormValid =
            startLocation.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(bottom = 96.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Join a Workbound Ride", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (showDetails) "Collapse" else "Expand"
                    )
                }
            }

            if (showDetails) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startLocation,
                        onValueChange = {
                            startLocation = it
                            coroutineScope.launch {
                                startLocationSuggestions =
                                    RepositoryProvider.mapsService.getAddressSuggestions(it)
                                expanded = startLocationSuggestions.isNotEmpty()
                            }
                        },
                        label = { Text("Start Location") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                textFieldSize = coordinates.size
                            }
                            .focusRequester(focusRequester),
                        singleLine = true,
                        interactionSource = remember { MutableInteractionSource() }
                    )

                    if (expanded && startLocationSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                                .padding(top = with(LocalDensity.current) { textFieldSize.height.toDp() })
                                .heightIn(max = 200.dp)
                        ) {
                            LazyColumn {
                                items(startLocationSuggestions) { suggestion ->
                                    ListItem(
                                        headlineContent = { Text(suggestion) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                startLocation = suggestion
                                                expanded = false
                                                coroutineScope.launch {
                                                    focusRequester.requestFocus()
                                                }
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = destination,
                    onValueChange = {},
                    label = { Text("Destination") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

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
                            val selectedCalendar = Calendar.getInstance()
                            selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                            selectedCalendar.set(Calendar.MINUTE, minute)

                            val todayStr =
                                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now.time)
                            if (selectedDate == todayStr && selectedCalendar.before(now)) {
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
                    Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick an Arrival Time")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val locationData =
                                RepositoryProvider.mapsService.getCoordinatesFromAddress(
                                    startLocation
                                )

                            if (locationData == null) {
                                Toast.makeText(context, "❌ Address not found", Toast.LENGTH_SHORT)
                                    .show()
                                return@launch
                            }

                            pickupStop = PickupStop(
                                location = locationData,
                                passengerId = uid
                            )

                            val availableRides = passengerRideFinder.getAvailableRidesForPassenger(
                                companyId = "company123",
                                direction = RideDirection.TO_WORK,
                                passengerArrivalTime = selectedTime,
                                passengerDepartureTime = "",
                                passengerDate = selectedDate,
                                pickupPoint = pickupStop,
                                rideRepository = rideRepository,
                                rideRequestRepository = RepositoryProvider.provideRideRequestRepository()
                            )

                            rides = availableRides
                            ridesFetched = true
                        } catch (e: Exception) {
                            Toast.makeText(context, "❌ Error fetching rides", Toast.LENGTH_SHORT)
                                .show()
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Show Available Rides")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
            ) {
                if (ridesFetched) {
                    if (rides.isEmpty()) {
                        Text(
                            "❌ No available rides matching your request.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rides) { ride ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Start: ${ride.ride.startLocation.name}")
                                        Text("Destination: ${ride.ride.destination.name}")
                                        Text("Date: ${ride.ride.date}")
                                        Text("Arrival Time: ${ride.ride.arrivalTime ?: ride.ride.departureTime}")
                                        Text("Pickup Time: ${ride.detourEvaluationResult.pickupLocation?.pickupTime ?: "לא ידוע"}")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
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
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Send Request")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
}