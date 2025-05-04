package com.wepool.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
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

    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var rides by remember { mutableStateOf<List<RideCandidate>>(emptyList()) }
    var ridesFetched by remember { mutableStateOf(false) }

    val passengerRideFinder = PassengerRideFinder(
        rideRepository = RepositoryProvider.provideRideRepository(),
        mapsService = RepositoryProvider.mapsService,
        routeMatcher = RouteMatcher
    )

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

        // Destination input with autocomplete
        Box {
            OutlinedTextField(
                value = destination,
                onValueChange = {
                    destination = it
                    expanded = true
                    coroutineScope.launch {
                        destinationSuggestions = RepositoryProvider.mapsService.getAddressSuggestions(it)
                    }
                },
                label = { Text("Destination") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            DropdownMenu(
                expanded = expanded && destinationSuggestions.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                destinationSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            destination = suggestion
                            expanded = false
                        }
                    )
                }
            }
        }

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

                        val pickupStop = PickupStop(
                            location = locationData,
                            passengerId = uid
                        )
                        val geoPoint = locationData.geoPoint
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)

                        val availableRides = passengerRideFinder.getAvailableRidesForPassenger(
                            companyId = "company123", // TODO: replace with actual
                            direction = RideDirection.TO_HOME,
                            passengerArrivalTime = "",
                            passengerDepartureTime = selectedTime,
                            passengerDate = selectedDate,
                            pickupPoint = pickupStop,
                           // passengerId = uid,
                            rideRepository = RepositoryProvider.provideRideRepository()
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
                                Text("Start: ${rideCandidate.ride.startLocation}")
                                Text("Destination: ${rideCandidate.ride.destination}")
                                Text("Date: ${rideCandidate.ride.date}")
                                Text("Time: ${rideCandidate.detourEvaluationResult.updatedReferenceTime}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        // TODO: Hook "Send Request"
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}