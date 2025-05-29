package com.wepool.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.wepool.app.data.model.logic.PolylineDecoder
import com.wepool.app.data.model.ride.PickupStop
import com.wepool.app.data.model.ride.Ride

enum class PointType { START, END, PICKUP_STOP, NONE }

@Composable
fun RideDynamicMap(ride: Ride, extraPickupStop: PickupStop? = null, modifier: Modifier = Modifier) {
    val startLatLng = LatLng(ride.startLocation.geoPoint.latitude, ride.startLocation.geoPoint.longitude)
    val endLatLng = LatLng(ride.destination.geoPoint.latitude, ride.destination.geoPoint.longitude)
    val centerLat = (startLatLng.latitude + endLatLng.latitude) / 2
    val centerLng = (startLatLng.longitude + endLatLng.longitude) / 2

    var selectedStop by remember { mutableStateOf<PickupStop?>(null) }
    var selectedPointType by remember { mutableStateOf(PointType.NONE) }
    var showInfoWindow by remember { mutableStateOf(false) }
    var selectedStopIndex by remember { mutableStateOf<Int?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 12f)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier) {
            GoogleMap(
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = true),
                modifier = Modifier.fillMaxSize()
            ) {
                Marker(
                    state = MarkerState(startLatLng),
                    title = "Start",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                    onClick = {
                        selectedStop = null
                        selectedPointType = PointType.START
                        showInfoWindow = true
                        true
                    }
                )

                Marker(
                    state = MarkerState(endLatLng),
                    title = "End",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                    onClick = {
                        selectedStop = null
                        selectedPointType = PointType.END
                        showInfoWindow = true
                        true
                    }
                )

                val isWorkbound = ride.direction?.name == "TO_WORK"
                val allStops = remember(ride.pickupStops to extraPickupStop) {
                    val combined = ride.pickupStops.toMutableList()
                    extraPickupStop?.let { combined.add(it) }
                    combined.sortedWith(compareBy {
                        if (isWorkbound) it.pickupTime ?: "" else it.dropoffTime ?: ""
                    })
                }

                allStops.forEachIndexed { index, stop ->
                    val pos = LatLng(stop.location.geoPoint.latitude, stop.location.geoPoint.longitude)
                    Marker(
                        state = MarkerState(pos),
                        title = "Stop Number: ${index + 1}",
                        snippet = "Pickup Stop: ${stop.location.name}",
                        icon = if (extraPickupStop == stop)
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                        else
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        onClick = {
                            selectedStop = stop
                            selectedStopIndex = index
                            selectedPointType = PointType.PICKUP_STOP
                            showInfoWindow = true
                            true
                        }
                    )
                }

                Polyline(PolylineDecoder.decodeAndSimplify(ride.encodedPolyline))
            }

            if (showInfoWindow) {
                val isWorkbound = ride.direction!!.name == "TO_WORK"

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .background(color = Color.White, shape = MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    Column {
                        when {
                            selectedStop != null -> {
                                if (isWorkbound) {
                                    Text("Stop Number: ${selectedStopIndex?.plus(1) ?: "?"}")
                                    Text("Pickup Location: ${selectedStop!!.location.name}")
                                    Text("Pickup Time: ${selectedStop!!.pickupTime ?: "N/A"}")
                                } else {
                                    Text("Stop Number: ${selectedStopIndex?.plus(1) ?: "?"}")
                                    Text("Dropoff Location: ${selectedStop!!.location.name}")
                                    Text("Dropoff Time: ${selectedStop!!.dropoffTime ?: "N/A"}")
                                }
                            }
                            selectedPointType == PointType.START -> {
                                Text("Start Location: ${ride.startLocation.name}")
                                Text("Departure Time: ${ride.departureTime ?: "N/A"}")
                            }
                            selectedPointType == PointType.END -> {
                                Text("Destination: ${ride.destination.name}")
                                Text("Arrival Time: ${ride.arrivalTime ?: "N/A"}")
                            }
                            else -> {
                                Text("No details available")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showInfoWindow = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}