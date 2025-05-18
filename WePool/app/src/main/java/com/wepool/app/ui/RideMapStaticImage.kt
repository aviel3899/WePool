package com.wepool.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.infrastructure.RepositoryProvider

@Composable
fun RideStaticMapImage(ride: Ride, modifier: Modifier = Modifier) {
    val mapsService = RepositoryProvider.mapsService
    val start = ride.startLocation.geoPoint
    val end = ride.destination.geoPoint

    val mapUrl = mapsService.getStaticMapUrlWithPolylineAndMarkers(
        encodedPolyline = ride.encodedPolyline,
        start = start,
        end = end,
        pickupStops = ride.pickupStops
    )

    AsyncImage(
        model = mapUrl,
        contentDescription = "Static Ride Map",
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentScale = ContentScale.Crop
    )
}
