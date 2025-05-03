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
fun PassengerHomeboundRideSearchScreen(navController: NavController, uid: String) {
    val context = LocalContext.current

    val fixedStartLocation = "מכללת אפקה"
    var destination by remember { mutableStateOf("") }
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

    // Update form validation
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

        // Start Location (fixed)
        OutlinedTextField(
            value = fixedStartLocation,
            onValueChange = {},
            label = { Text("Start Location") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Destination (user input)
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Date Picker
        Button(onClick = {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                context,
                { _, year, month, day ->
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    calendar.set(year, month, day)
                    selectedDate = sdf.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.datePicker.minDate = calendar.timeInMillis
            datePicker.show()
        }) {
            Text(if (selectedDate.isNotBlank()) "Selected Date: $selectedDate" else "Pick a Date")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time Picker
        Button(onClick = {
            val now = Calendar.getInstance()
            val toast = Toast.makeText(context, "", Toast.LENGTH_SHORT)

            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    selectedCalendar.set(Calendar.MINUTE, minute)
                    selectedCalendar.set(Calendar.SECOND, 0)

                    val todayStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now.time)

                    if (selectedDate == todayStr && selectedCalendar.before(now)) {
                        toast.setText("❌ Please select a future time.")
                        toast.show()
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

        // Show Available Rides Button
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val locationData = RepositoryProvider.mapsService.getCoordinatesFromAddress(destination)

                        if (locationData == null) {
                            Toast.makeText(context, "❌ Address not found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val geoPoint = locationData.geoPoint
                        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)

                        val availableRides = passengerRideFinder.getAvailableRidesForPassenger(
                            companyId = "company123", // change later
                            direction = RideDirection.TO_HOME,
                            passengerArrivalTime = "",
                            passengerDepartureTime = selectedTime,
                            passengerDate = selectedDate,
                            pickupPoint = latLng, // now real LatLng coordinates!!
                            passengerId = uid,
                            rideRepository = RepositoryProvider.provideRideRepository()
                        )

                        rides = availableRides // Update your rides list here to show in LazyColumn
                        ridesFetched = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "❌ Error fetching rides", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            },
            enabled = isFormValid, // your validation logic
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Available Rides")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rides List
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
                                //Text("Time: ${rideCandidate.ride.arrivalTime ?: rideCandidate.ride.departureTime}")
                               Text("Time: ${rideCandidate.detourEvaluationResult.updatedReferenceTime}")
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        // TODO: Hook "Send Request" later
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

        // Back button
        OutlinedButton(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}