package com.wepool.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
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
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PassengerWorkboundRideSearchScreen(navController: NavController, uid: String) {
    val context = LocalContext.current

    var startLocation by remember { mutableStateOf("") }
    val destination = "מכללת אפקה"
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var isFormValid by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val passengerRideFinder = PassengerRideFinder(
        rideRepository = RepositoryProvider.provideRideRepository(),
        mapsService = RepositoryProvider.mapsService,
        routeMatcher = RouteMatcher
    )

    var rides by remember { mutableStateOf<List<RideCandidate>>(emptyList()) }
    var ridesFetched by remember { mutableStateOf(false) }

    LaunchedEffect(startLocation, selectedDate, selectedTime) {
        isFormValid = startLocation.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Join a Workbound Ride", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = startLocation,
            onValueChange = { startLocation = it },
            label = { Text("Start Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = destination,
            onValueChange = {},
            label = { Text("Destination") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
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
            Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick an Arrival Time")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val locationData = RepositoryProvider.mapsService.getCoordinatesFromAddress(startLocation)

                        if (locationData == null) {
                            Toast.makeText(context, "❌ Address not found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val latLng = LatLng(locationData.geoPoint.latitude, locationData.geoPoint.longitude)

                        val availableRides = passengerRideFinder.getAvailableRidesForPassenger(
                            companyId = "company123", // change later
                            direction = RideDirection.TO_WORK,
                            passengerArrivalTime = selectedTime,
                            passengerDepartureTime = "",
                            passengerDate = selectedDate,
                            pickupPoint = latLng,
                            passengerId = uid,
                            rideRepository = RepositoryProvider.provideRideRepository()
                        )

                        rides = availableRides
                        ridesFetched = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "❌ Error fetching rides", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Available Rides")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (ridesFetched) {
            if (rides.isEmpty()) {
                Text("❌ No available rides matching your request.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rides) { ride ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Start: ${ride.ride.startLocation}")
                                Text("Destination: ${ride.ride.destination}")
                                Text("Date: ${ride.ride.date}")
                                Text("Time: ${ride.ride.arrivalTime ?: ride.ride.departureTime}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        // Hook Send Request later
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
