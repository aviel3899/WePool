package com.wepool.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wepool.app.data.model.enums.RideDirection
import com.wepool.app.infrastructure.RepositoryProvider
import kotlinx.coroutines.launch

@Composable
fun HomeboundRideCreationScreen(navController: NavController, uid: String) {
    val context = LocalContext.current

    val startLocation = "מכללת אפקה"
    var destination by remember { mutableStateOf("") }
    var seatsAvailable by remember { mutableStateOf(1) }
    var maxDetour by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val seatOptions = listOf(1, 2, 3, 4)
    val coroutineScope = rememberCoroutineScope()
    val rideRepository = RepositoryProvider.provideRideRepository()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Create Homebound Ride", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // Fixed Start Location
        OutlinedTextField(
            value = startLocation,
            onValueChange = {},
            label = { Text("Start Location") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Destination
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Dropdown for Seats Available
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

        // Max Detour
        OutlinedTextField(
            value = maxDetour,
            onValueChange = { maxDetour = it },
            label = { Text("Max Detour (minutes)") },
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
            datePicker.datePicker.minDate = calendar.timeInMillis // Only future dates
            datePicker.show()
        }) {
            Text(if (selectedDate.isNotBlank()) "Selected Date: $selectedDate" else "Pick a Date")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time Picker
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
            Text(if (selectedTime.isNotBlank()) "Selected Time: $selectedTime" else "Pick a Time")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
         Button(
             onClick = {
                 coroutineScope.launch {
                     val success = rideRepository.planRideFromUserInput(
                         driverId = uid,
                         companyId = "company123", // You can make this dynamic later
                         startAddress = "מכללת אפקה",
                         destinationAddress = destination,
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
                         navController.popBackStack()
                     } else {
                         Log.e("RideCreation", "❌ Failed to save ride")
                     }
                 }
             },
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
        }
}

