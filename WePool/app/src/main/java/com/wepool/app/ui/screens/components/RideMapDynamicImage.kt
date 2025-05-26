package com.wepool.app.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.wepool.app.data.model.logic.PolylineDecoder
import com.wepool.app.data.model.ride.Ride
import com.google.maps.android.compose.Polyline
import com.wepool.app.data.model.ride.PickupStop


enum class PointType { START, END, PICKUP_STOP, NONE }

@Composable
fun RideDynamicMap(ride: Ride, modifier: Modifier = Modifier) {
    val startLatLng =
        LatLng(ride.startLocation.geoPoint.latitude, ride.startLocation.geoPoint.longitude)
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

                ride.pickupStops.forEachIndexed { index, stop ->
                    val pos =
                        LatLng(stop.location.geoPoint.latitude, stop.location.geoPoint.longitude)
                    val stopNumber = index + 1
                    Marker(
                        state = MarkerState(pos),
                        title = "Stop Number: $stopNumber",
                        snippet = "Pickup Stop: ${stop.location.name}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
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

            // Custom InfoWindow UI
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



