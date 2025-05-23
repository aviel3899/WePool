package com.wepool.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Home
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
import com.wepool.app.data.model.ride.RideCandidate
import com.wepool.app.data.model.logic.RouteMatcher
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun PassengerHomeboundRideSearchScreen(navController: NavController, uid: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val fixedStartLocation = "מכללת אפקה"
    var destination by remember { mutableStateOf("") }
    var destinationSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }

    val focusRequester = remember { FocusRequester() }

    var passengerNotes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var rides by remember { mutableStateOf<List<RideCandidate>>(emptyList()) }
    var ridesFetched by remember { mutableStateOf(false) }

    var showDetails by remember { mutableStateOf(true) }

    val rideRequestRepository = RepositoryProvider.provideRideRequestRepository()
    val passengerRideFinder = PassengerRideFinder(
        mapsService = RepositoryProvider.mapsService,
        routeMatcher = RouteMatcher
    )

    var pickupStop by remember { mutableStateOf(PickupStop()) }

    LaunchedEffect(destination, selectedDate, selectedTime) {
        isFormValid = destination.isNotBlank() &&
                selectedDate.isNotBlank() &&
                selectedTime.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Join a Homebound Ride", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { showDetails = !showDetails }) {
                Icon(
                    imageVector = if (showDetails) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (showDetails) "Collapse" else "Expand"
                )
            }
        }

        if (showDetails) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = fixedStartLocation,
                onValueChange = {},
                label = { Text("Start Location") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = {
                        destination = it
                        coroutineScope.launch {
                            destinationSuggestions = RepositoryProvider.mapsService.getAddressSuggestions(it)
                            expanded = destinationSuggestions.isNotEmpty()
                        }
                    },
                    label = { Text("Destination") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size
                        }
                        .focusRequester(focusRequester),
                    singleLine = true,
                    interactionSource = remember { MutableInteractionSource() }
                )

                if (expanded && destinationSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                            .padding(top = with(LocalDensity.current) { textFieldSize.height.toDp() })
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn {
                            items(destinationSuggestions) { suggestion ->
                                ListItem(
                                    headlineContent = { Text(suggestion) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            destination = suggestion
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

                        val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now.time)
                        if (selectedDate == todayStr && selectedCalendar.before(now)) {
                            Toast.makeText(context, "❌ Please select a future time.", Toast.LENGTH_SHORT).show()
                        } else {
                            selectedTime = String.format("%02d:%02d", hour, minute)
                        }
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            }) {
                Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick a Departure Time")
            }
        }

    /*Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Join a Homebound Ride", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fixedStartLocation,
            onValueChange = {},
            label = { Text("Start Location") },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = destination,
                onValueChange = {
                    destination = it
                    coroutineScope.launch {
                        destinationSuggestions = RepositoryProvider.mapsService.getAddressSuggestions(it)
                        expanded = destinationSuggestions.isNotEmpty()
                    }
                },
                label = { Text("Destination") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        textFieldSize = coordinates.size
                    }
                    .focusRequester(focusRequester),
                singleLine = true,
                interactionSource = remember { MutableInteractionSource() }
            )

            if (expanded && destinationSuggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                        .padding(top = with(LocalDensity.current) { textFieldSize.height.toDp() })
                        .heightIn(max = 200.dp)
                ) {
                    LazyColumn {
                        items(destinationSuggestions) { suggestion ->
                            ListItem(
                                headlineContent = { Text(suggestion) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        destination = suggestion
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

                    val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now.time)
                    if (selectedDate == todayStr && selectedCalendar.before(now)) {
                        Toast.makeText(context, "❌ Please select a future time.", Toast.LENGTH_SHORT).show()
                    } else {
                        selectedTime = String.format("%02d:%02d", hour, minute)
                    }
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
            ).show()
        }) {
            Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick a Departure Time")
        }*/

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    try {
                        val locationData = RepositoryProvider.mapsService.getCoordinatesFromAddress(destination)

                        if (locationData == null) {
                            Toast.makeText(context, "❌ Address not found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        pickupStop = PickupStop(
                            location = locationData,
                            passengerId = uid
                        )

                        val availableRides = passengerRideFinder.getAvailableRidesForPassenger(
                            companyId = "company123",
                            direction = RideDirection.TO_HOME,
                            passengerArrivalTime = "",
                            passengerDepartureTime = selectedTime,
                            passengerDate = selectedDate,
                            pickupPoint = pickupStop,
                            rideRepository = RepositoryProvider.provideRideRepository(),
                            rideRequestRepository = RepositoryProvider.provideRideRequestRepository()
                        )

                        rides = availableRides
                        ridesFetched = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "❌ Error fetching rides", Toast.LENGTH_SHORT).show()
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
                Text("Searching...")
            } else {
                Text("Show Available Rides")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (ridesFetched) {
                if (rides.isEmpty()) {
                    Text(
                        "❌ No available rides matching your request.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(rides) { rideCandidate ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Start: ${rideCandidate.ride.startLocation.name}")
                                    Text("Destination: ${rideCandidate.ride.destination.name}")
                                    Text("Date: ${rideCandidate.ride.date}")
                                    Text("Arrival Time: ${rideCandidate.detourEvaluationResult.updatedReferenceTime}")
                                    Text("Dropoff Time: ${rideCandidate.detourEvaluationResult.pickupLocation?.dropoffTime ?: "לא ידוע"}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val success = rideRequestRepository.sendRequest(
                                                    rideId = rideCandidate.ride.rideId,
                                                    passengerId = uid,
                                                    pickupLocation = pickupStop.location,
                                                    detourEvaluationResult = rideCandidate.detourEvaluationResult
                                                )
                                                Toast.makeText(
                                                    context,
                                                    if (success) "✅ Request sent" else "❌ Failed to send request",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                if (success) {
                                                    // Remove this rideCandidate from the list
                                                    rides = rides.filterNot { it.ride.rideId == rideCandidate.ride.rideId }
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                navController.navigate("intermediate/$uid?fromLogin=false") {
                    popUpTo("intermediate/$uid?fromLogin=false") {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant, // soft neutral
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant // high contrast
            )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Back to Home", style = MaterialTheme.typography.labelLarge)
        }

    }
}