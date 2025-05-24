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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.components.FavoriteLocationDropdown

@Composable
fun HomeboundRideCreationScreen(navController: NavController, uid: String) {
    val context = LocalContext.current
    val startLocation by remember { mutableStateOf(LocationData(name = "מכללת אפקה")) }
    var destination by remember { mutableStateOf(LocationData()) }
    var seatsAvailable by remember { mutableStateOf(1) }
    var maxDetour by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val seatOptions = listOf(1, 2, 3, 4)
    val coroutineScope = rememberCoroutineScope()
    val rideRepository = RepositoryProvider.provideRideRepository()
    val mapsService = RepositoryProvider.mapsService
    val userRepository = RepositoryProvider.provideUserRepository()

    val isFormValid = destination.name.isNotBlank() &&
            maxDetour.isNotBlank() &&
            selectedDate.isNotBlank() &&
            selectedTime.isNotBlank()

    var destinationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var autoExpanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }
    var showDetails by remember { mutableStateOf(true) }

    var favoriteLocations by remember { mutableStateOf<List<LocationData>>(emptyList()) }
    LaunchedEffect(uid) {
        coroutineScope.launch {
            val user = userRepository.getUser(uid)
            favoriteLocations = user?.favoriteLocations ?: emptyList()
        }
    }

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
                Text("Create Homebound Ride", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { showDetails = !showDetails }) {
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (showDetails) "Collapse" else "Expand"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (showDetails) {
                OutlinedTextField(
                    value = startLocation.name,
                    onValueChange = {},
                    label = { Text("Start Location") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                FavoriteLocationDropdown(
                    label = "Destination",
                    locationData = destination,
                    onLocationSelected = { selected ->
                        destination = selected
                    },
                    onTextChanged = { text ->
                        destination = destination.copy(name = text)
                        coroutineScope.launch {
                            destinationSuggestions = mapsService.getAddressSuggestions(text)
                            autoExpanded = destinationSuggestions.isNotEmpty()
                        }
                    },
                    suggestions = destinationSuggestions,
                    favoriteLocations = favoriteLocations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates -> textFieldSize = coordinates.size }
                        .focusRequester(focusRequester)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = seatsAvailable.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Seats Available") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand Seats Dropdown"
                                )
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
                            selectedTime = String.format("%02d:%02d", hour, minute)
                        },
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        true
                    ).show()
                }) {
                    Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick a departure Time")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val destinationData =
                            mapsService.getCoordinatesFromAddress(destination.name)!!
                        val startLocationData =
                            mapsService.getCoordinatesFromAddress(startLocation.name)!!

                        val success = rideRepository.planRideFromUserInput(
                            driverId = uid,
                            companyId = "company123",
                            startAddress = startLocationData,
                            destinationAddress = destinationData,
                            departureTime = selectedTime,
                            date = selectedDate,
                            direction = RideDirection.TO_HOME,
                            availableSeats = seatsAvailable,
                            occupiedSeats = 0,
                            maxDetourMinutes = maxDetour.toIntOrNull() ?: 0,
                            notes = ""
                        )
                        if (success) {
                            Log.d("RideCreation", "✅ Ride saved successfully")
                            Toast.makeText(context, "Ride created successfully", Toast.LENGTH_SHORT)
                                .show()
                            navController.navigate("driverMenu/$uid") {
                                popUpTo("homeboundRide/$uid") { inclusive = true }
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
}