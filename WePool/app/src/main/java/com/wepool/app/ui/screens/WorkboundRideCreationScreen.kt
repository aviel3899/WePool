package com.wepool.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkboundRideCreationScreen(navController: NavController, uid: String) {
    val context = LocalContext.current

    var startLocation by remember { mutableStateOf(LocationData()) }
    val destination by remember { mutableStateOf(LocationData(name = "מכללת אפקה")) }
    var seatsAvailable by remember { mutableStateOf(1) }
    var maxDetour by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val seatOptions = listOf(1, 2, 3, 4)
    val rideRepository = RepositoryProvider.provideRideRepository()
    val mapsService = RepositoryProvider.mapsService
    val coroutineScope = rememberCoroutineScope()

    val isFormValid = startLocation.name.isNotBlank() &&
            maxDetour.isNotBlank() &&
            selectedDate.isNotBlank() &&
            selectedTime.isNotBlank()

    var startLocationSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var autoExpanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val focusRequester = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        item {
            Text("Create Workbound Ride", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startLocation.name,
                    onValueChange = {
                        startLocation = startLocation.copy(name = it)
                        coroutineScope.launch {
                            startLocationSuggestions =
                                RepositoryProvider.mapsService.getAddressSuggestions(it)
                            autoExpanded = startLocationSuggestions.isNotEmpty()
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

                if (autoExpanded && startLocationSuggestions.isNotEmpty()) {
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
                                            startLocation.name = suggestion
                                            autoExpanded = false
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
        }

        item {
            OutlinedTextField(
                value = destination.name,
                onValueChange = {},
                label = { Text("Destination") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
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
        }

        item {
            OutlinedTextField(
                value = maxDetour,
                onValueChange = { maxDetour = it },
                label = { Text("Max Detour (minutes)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
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
        }

        item {
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
                Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick an arrival Time")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val startLocationData = mapsService.getCoordinatesFromAddress(startLocation.name)!!
                        val destinationData = mapsService.getCoordinatesFromAddress(destination.name)!!

                        val success = rideRepository.planRideFromUserInput(
                            driverId = uid,
                            companyId = "company123",
                            startAddress = startLocationData,
                            destinationAddress = destinationData,
                            arrivalTime = selectedTime,
                            date = selectedDate,
                            direction = RideDirection.TO_WORK,
                            availableSeats = seatsAvailable,
                            occupiedSeats = 0,
                            maxDetourMinutes = maxDetour.toIntOrNull() ?: 0,
                            notes = ""
                        )
                        if (success) {
                            Log.d("RideCreation", "✅ Ride saved successfully")
                            Toast.makeText(context, "Ride created successfully", Toast.LENGTH_SHORT).show()
                            navController.navigate("driverMenu/$uid") {
                                popUpTo("workboundRide/$uid") {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            Log.e("RideCreation", "❌ Failed to save ride")
                            Toast.makeText(context, "Failed to create ride. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    navController.popBackStack()
                },
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
}