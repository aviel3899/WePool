package com.wepool.app.data.model.ride

import com.wepool.app.data.model.enums.ride.RideDirection
import com.wepool.app.data.model.enums.ride.RideSortFieldsWithOrder

data class RideSearchFilters(
    val companyName: String? = null,
    val userNameOrEmail: String? = null,
    val userDisplay: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val timeFrom: String? = null,
    val timeTo: String? = null,
    val direction: RideDirection? = null,
    val sortFields: List<RideSortFieldsWithOrder> = emptyList(),
    val isActiveRide: Boolean? = null,
)
