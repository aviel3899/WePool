package com.wepool.app.ui.screens.driverScreens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.infrastructure.RepositoryProvider
import com.wepool.app.ui.components.BackgroundWrapper
import com.wepool.app.ui.screens.components.BottomNavigationButtons
import com.wepool.app.ui.screens.favoriteLocations.FavoriteLocationDropdown
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RideCreationScreen(navController: NavController, uid: String, direction: RideDirection) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isToWork = direction == RideDirection.TO_WORK

    var fixedLocation by remember { mutableStateOf<LocationData?>(null) }
    var companyCode by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf(LocationData()) }
    var destination by remember { mutableStateOf(LocationData()) }
    var seatsAvailable by remember { mutableStateOf(1) }
    var maxDetour by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var locationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var favoriteLocations by remember { mutableStateOf<List<LocationData>>(emptyList()) }

    val rideRepository = RepositoryProvider.provideRideRepository()
    val mapsService = RepositoryProvider.mapsService
    val userRepository = RepositoryProvider.provideUserRepository()

    val isFormValid = startLocation.name.isNotBlank() &&
            destination.name.isNotBlank() &&
            maxDetour.isNotBlank() &&
            selectedDate.isNotBlank() &&
            selectedTime.isNotBlank()

    LaunchedEffect(uid) {
        coroutineScope.launch {
            val user = userRepository.getUser(uid) ?: return@launch
            favoriteLocations = user.favoriteLocations ?: emptyList()
            companyCode = user.companyCode
            fixedLocation = RepositoryProvider.provideCompanyRepository()
                .getLocationByCompanyCode(companyCode)
        }
    }

    LaunchedEffect(fixedLocation) {
        fixedLocation?.let {
            if (isToWork) destination = it else startLocation = it
        }
    }

    BackgroundWrapper {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            if (fixedLocation != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 80.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (isToWork) "Create Workbound Ride" else "Create Homebound Ride",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isToWork) {
                            FavoriteLocationDropdown(
                                label = "Start Location",
                                locationData = startLocation,
                                onLocationSelected = { startLocation = it },
                                onTextChanged = {
                                    startLocation = startLocation.copy(name = it)
                                    coroutineScope.launch {
                                        locationSuggestions = mapsService.getAddressSuggestions(it)
                                    }
                                },
                                suggestions = locationSuggestions,
                                favoriteLocations = favoriteLocations
                            )
                        } else {
                            OutlinedTextField(
                                value = startLocation.name,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Start Location") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isToWork) {
                            FavoriteLocationDropdown(
                                label = "Destination",
                                locationData = destination,
                                onLocationSelected = { destination = it },
                                onTextChanged = {
                                    destination = destination.copy(name = it)
                                    coroutineScope.launch {
                                        locationSuggestions = mapsService.getAddressSuggestions(it)
                                    }
                                },
                                suggestions = locationSuggestions,
                                favoriteLocations = favoriteLocations
                            )
                        } else {
                            OutlinedTextField(
                                value = destination.name,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Destination") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = seatsAvailable.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Seats Available") },
                                trailingIcon = {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (seatsAvailable != 1) {
                                IconButton(onClick = { seatsAvailable = 1 }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Seats",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            (1..4).forEach {
                                DropdownMenuItem(
                                    text = { Text(it.toString()) },
                                    onClick = {
                                        seatsAvailable = it
                                        expanded = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = maxDetour,
                                onValueChange = { maxDetour = it },
                                label = { Text("Max Detour (minutes)") },
                                modifier = Modifier.weight(1f)
                            )
                            if (maxDetour.isNotBlank()) {
                                IconButton(onClick = { maxDetour = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Detour",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedButton(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                calendar.set(y, m, d)
                                                selectedDate =
                                                    SimpleDateFormat(
                                                        "dd-MM-yyyy",
                                                        Locale.getDefault()
                                                    )
                                                        .format(calendar.time)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.size(80.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Pick a Date",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (selectedDate.isNotBlank()) selectedDate else "Pick a Date",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    if (selectedDate.isNotBlank()) {
                                        IconButton(
                                            onClick = { selectedDate = "" },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Clear Date",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OutlinedButton(
                                    onClick = {
                                        val calendar = Calendar.getInstance()
                                        TimePickerDialog(
                                            context,
                                            { _, h, m ->
                                                selectedTime = String.format("%02d:%02d", h, m)
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    modifier = Modifier.size(80.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Pick a Time",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (selectedTime.isNotBlank()) selectedTime
                                        else if (isToWork) "Pick an\narrival time"
                                        else "Pick a\ndeparture time",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.align(Alignment.Center),
                                        textAlign = TextAlign.Center
                                    )
                                    if (selectedTime.isNotBlank()) {
                                        IconButton(
                                            onClick = { selectedTime = "" },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Clear Time",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            if (isToWork) {
                                startLocation = LocationData()
                            } else {
                                destination = LocationData()
                            }
                            seatsAvailable = 1
                            maxDetour = ""
                            selectedDate = ""
                            selectedTime = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Clear All Fields")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            coroutineScope.launch {
                                val calendar = Calendar.getInstance()
                                val formatter =
                                    SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                                val fullDateTime = "$selectedDate $selectedTime"
                                val rideTime = try {
                                    formatter.parse(fullDateTime)?.time ?: 0L
                                } catch (e: Exception) {
                                    0L
                                }

                                val now = System.currentTimeMillis()
                                val timeDiffMinutes = (rideTime - now) / 60000

                                val isValidTime =
                                    if (isToWork) timeDiffMinutes >= 120 else timeDiffMinutes >= 10

                                if (!isValidTime) {
                                    Toast.makeText(
                                        context,
                                        "Select later time.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                val start =
                                    mapsService.getCoordinatesFromAddress(startLocation.name)
                                        ?: return@launch
                                val end = mapsService.getCoordinatesFromAddress(destination.name)
                                    ?: return@launch

                                val success = rideRepository.planRideFromUserInput(
                                    driverId = uid,
                                    companyCode = companyCode,
                                    startAddress = start,
                                    destinationAddress = end,
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
                                    Toast.makeText(
                                        context,
                                        "❌ Failed to create ride",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }, enabled = isFormValid, modifier = Modifier.fillMaxWidth()) {
                            Text("Save")
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp
                    ) {
                        BottomNavigationButtons(
                            uid = uid,
                            navController = navController,
                            showBackButton = true,
                            showHomeButton = true
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}