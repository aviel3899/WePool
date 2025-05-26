package com.wepool.app.ui.screens.driverScreens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.FavoriteLocationDropdown
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RideCreationScreen(navController: NavController, uid: String, direction: RideDirection) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fixedLocation by remember { mutableStateOf<LocationData?>(null) }
    val isToWork = direction == RideDirection.TO_WORK
    val title = if (isToWork) "Create Workbound Ride" else "Create Homebound Ride"

    var companyCode by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf(LocationData()) }
    var destination by remember { mutableStateOf(LocationData()) }
    var seatsAvailable by remember { mutableStateOf(1) }
    var maxDetour by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(true) }

    val seatOptions = listOf(1, 2, 3, 4)
    val rideRepository = RepositoryProvider.provideRideRepository()
    val mapsService = RepositoryProvider.mapsService
    val userRepository = RepositoryProvider.provideUserRepository()

    val isFormValid = startLocation.name.isNotBlank() &&
            destination.name.isNotBlank() &&
            maxDetour.isNotBlank() &&
            selectedDate.isNotBlank() &&
            selectedTime.isNotBlank()

    var locationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var favoriteLocations by remember { mutableStateOf<List<LocationData>>(emptyList()) }

    LaunchedEffect(uid) {
        coroutineScope.launch {
            try {
                val user = userRepository.getUser(uid)
                if (user == null) {
                    Log.e("RideCreation", "❌ User is null")
                    return@launch
                }

                favoriteLocations = user.favoriteLocations ?: emptyList()
                companyCode = user.companyCode
                if (companyCode.isBlank()) {
                    Log.e("RideCreation", "❌ Company code is blank")
                    return@launch
                }

                val location = RepositoryProvider.provideCompanyRepository()
                    .getLocationByCompanyCode(companyCode)
                if (location == null) {
                    Log.e("RideCreation", "❌ Location not found for company code: $companyCode")
                } else {
                    fixedLocation = location
                    Log.d("RideCreation", "✅ Loaded fixedLocation: ${location.name}")
                }
            } catch (e: Exception) {
                Log.e("RideCreation", "❌ Error in LaunchedEffect: ${e.message}", e)
            }
        }
    }

    LaunchedEffect(fixedLocation) {
        if (fixedLocation != null) {
            if (isToWork) {
                destination = fixedLocation!!
            } else {
                startLocation = fixedLocation!!
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        if (fixedLocation != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
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

                    Spacer(modifier = Modifier.height(24.dp))

                    if (showDetails) {
                        // Start Location (always on top)
                        if (isToWork) {
                            FavoriteLocationDropdown(
                                label = "Start Location",
                                locationData = startLocation,
                                onLocationSelected = { startLocation = it },
                                onTextChanged = { text ->
                                    startLocation = startLocation.copy(name = text)
                                    coroutineScope.launch {
                                        locationSuggestions =
                                            mapsService.getAddressSuggestions(text)
                                    }
                                },
                                suggestions = locationSuggestions,
                                favoriteLocations = favoriteLocations,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = startLocation.name,
                                onValueChange = {},
                                label = { Text("Start Location") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Destination (always below)
                        if (!isToWork) {
                            FavoriteLocationDropdown(
                                label = "Destination",
                                locationData = destination,
                                onLocationSelected = { destination = it },
                                onTextChanged = { text ->
                                    destination = destination.copy(name = text)
                                    coroutineScope.launch {
                                        locationSuggestions =
                                            mapsService.getAddressSuggestions(text)
                                    }
                                },
                                suggestions = locationSuggestions,
                                favoriteLocations = favoriteLocations,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = destination.name,
                                onValueChange = {},
                                label = { Text("Destination") },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = seatsAvailable.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Seats Available") },
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                seatOptions.forEach { seat ->
                                    DropdownMenuItem(
                                        text = { Text(seat.toString()) },
                                        onClick = {
                                            seatsAvailable = seat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = maxDetour,
                            onValueChange = { maxDetour = it },
                            label = { Text("Max Detour (minutes)") },
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
                                            "\u274C Please select a future time.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        selectedTime =
                                            String.format("%02d:%02d", hour, minute).trim()
                                    }
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                true
                            ).show()
                        }) {
                            Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick a ${if (isToWork) "arrival" else "departure"} Time")
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = {
                            val selectedCalendar = Calendar.getInstance()
                            val selectedHourMin = selectedTime.split(":")
                            selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHourMin[0].toInt())
                            selectedCalendar.set(Calendar.MINUTE, selectedHourMin[1].toInt())

                            val now = Calendar.getInstance()
                            val timeDifference = selectedCalendar.timeInMillis - now.timeInMillis

                            Log.d("TimeDifference", "Time difference in milliseconds: $timeDifference")
                            val minimumGapMillis = if (isToWork) 60 * 60 * 1000 else 10 * 60 * 1000

                            if (timeDifference < minimumGapMillis) {
                                Toast.makeText(
                                    context,
                                    "Select later time",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                coroutineScope.launch {
                                    val startLocationData =
                                        mapsService.getCoordinatesFromAddress(startLocation.name)!!
                                    val destinationData =
                                        mapsService.getCoordinatesFromAddress(destination.name)!!

                                    val success = rideRepository.planRideFromUserInput(
                                        driverId = uid,
                                        companyCode = companyCode,
                                        startAddress = startLocationData,
                                        destinationAddress = destinationData,
                                        arrivalTime = if (isToWork) selectedTime else "",
                                        departureTime = if (!isToWork) selectedTime else "",
                                        date = selectedDate,
                                        direction = direction,
                                        availableSeats = seatsAvailable,
                                        occupiedSeats = 0,
                                        maxDetourMinutes = maxDetour.toIntOrNull() ?: 0,
                                        notes = ""
                                    )

                                    if (success) {
                                        Log.d("RideCreation", "✅ Ride saved successfully")
                                        Toast.makeText(
                                            context,
                                            "Ride created successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navController.navigate("driverMenu/$uid") {
                                            popUpTo("rideCreation/$uid/${direction.name}") {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        Log.e("RideCreation", "❌ Failed to save ride")
                                        Toast.makeText(
                                            context,
                                            "Failed to create ride. Please try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
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