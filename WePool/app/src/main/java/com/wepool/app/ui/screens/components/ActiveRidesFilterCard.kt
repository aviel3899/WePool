package com.wepool.app.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wepool.app.data.model.enums.RideDirection
import android.util.Log
import com.wepool.app.data.model.ride.Ride
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ActiveRidesFilterCard(
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String,
    selectedDirection: RideDirection?,
    directionMenuExpanded: Boolean,
    directionOptions: List<Pair<RideDirection, String>>,
    onShowDateRangePicker: () -> Unit,
    onShowTimeRangePicker: () -> Unit,
    onClearDateRange: () -> Unit,
    onClearTimeRange: () -> Unit,
    onClearDirection: () -> Unit,
    onDirectionSelected: (RideDirection) -> Unit,
    onDirectionMenuExpand: () -> Unit,
    onDirectionMenuDismiss: () -> Unit,
    onApplyFilter: () -> Unit
) {
    var filterExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { filterExpanded = !filterExpanded },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (filterExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (filterExpanded) "Collapse Filter" else "Expand Filter"
                    )
                }

                Text(
                    text = "Filter Your Rides",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (filterExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    OutlinedButton(
                        onClick = onShowDateRangePicker,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Date Range", fontSize = 18.sp, maxLines = 1)
                    }

                    if (startDate.isNotBlank() || endDate.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onClearDateRange, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear date range")
                        }
                    }
                }

                if (startDate.isNotBlank() && endDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "From $startDate to $endDate",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    OutlinedButton(
                        onClick = onShowTimeRangePicker,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("Time Range", fontSize = 18.sp, maxLines = 1)
                    }

                    if (startTime.isNotBlank() || endTime.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onClearTimeRange, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear time range")
                        }
                    }
                }

                if (startTime.isNotBlank() || endTime.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "From $startTime to $endTime",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        OutlinedButton(
                            onClick = onDirectionMenuExpand,
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = directionOptions.firstOrNull { it.first == selectedDirection }?.second
                                        ?: "Select Direction",
                                    modifier = Modifier.align(Alignment.Center),
                                    fontSize = 18.sp,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                                )
                            }
                        }

                        if (selectedDirection != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onClearDirection,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear direction"
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = directionMenuExpanded,
                        onDismissRequest = onDirectionMenuDismiss,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        directionOptions.forEach { (direction, label) ->
                            DropdownMenuItem(
                                text = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label)
                                    }
                                },
                                onClick = {
                                    onDirectionSelected(direction)
                                    onDirectionMenuDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onApplyFilter,
                    modifier = Modifier.fillMaxWidth(0.75f).height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Filter")
                }
            }
        }
    }
}

fun filterRides(
    allRides: List<Ride>,
    startDate: String,
    endDate: String,
    startTime: String,
    endTime: String,
    selectedDirection: RideDirection?
): List<Ride> {
    val formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val formatter2 = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    return allRides.filter { ride ->
        try {
            val rideDate = try {
                LocalDate.parse(ride.date, formatter1)
            } catch (e: Exception) {
                LocalDate.parse(ride.date, formatter2)
            }

            val departureTime = LocalTime.parse(ride.departureTime)
            val arrivalTime = LocalTime.parse(ride.arrivalTime)

            val rideDepartureDateTime = LocalDateTime.of(rideDate, departureTime)
            val rideArrivalDateTime = LocalDateTime.of(rideDate, arrivalTime)

            val startDateParsed =
                if (startDate.isNotBlank()) LocalDate.parse(startDate, formatter1) else null
            val endDateParsed =
                if (endDate.isNotBlank()) LocalDate.parse(endDate, formatter1) else null
            val startTimeParsed =
                if (startTime.isNotBlank()) LocalTime.parse(startTime) else null
            val endTimeParsed = if (endTime.isNotBlank()) LocalTime.parse(endTime) else null

            val minDepartureDateTime = when {
                startDateParsed != null && startTimeParsed != null -> LocalDateTime.of(
                    startDateParsed,
                    startTimeParsed
                )

                startDateParsed != null -> LocalDateTime.of(startDateParsed, LocalTime.MIN)
                else -> null
            }

            val maxArrivalDateTime = when {
                endDateParsed != null && endTimeParsed != null -> LocalDateTime.of(
                    endDateParsed,
                    endTimeParsed
                )

                endDateParsed != null -> LocalDateTime.of(endDateParsed, LocalTime.MAX)
                else -> null
            }

            val validDeparture =
                minDepartureDateTime == null || rideDepartureDateTime >= minDepartureDateTime
            val validArrival =
                maxArrivalDateTime == null || rideArrivalDateTime <= maxArrivalDateTime
            val directionMatches =
                selectedDirection == null || ride.direction == selectedDirection

            Log.d(
                "FilterDebug",
                "✅ Ride ${ride.rideId} | Departure: $rideDepartureDateTime | Arrival: $rideArrivalDateTime | " +
                        "ValidDeparture=$validDeparture | ValidArrival=$validArrival | DirectionMatch=$directionMatches"
            )

            validDeparture && validArrival && directionMatches
        } catch (e: Exception) {
            Log.e("FilterDebug", "❌ Failed to parse ride: ${ride.rideId} | ${e.message}")
            false
        }
    }
}