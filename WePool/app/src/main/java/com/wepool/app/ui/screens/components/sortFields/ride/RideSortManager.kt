package com.wepool.app.ui.screens.components.sortFields.ride

import com.wepool.app.data.model.enums.SortOrder
import com.wepool.app.data.model.enums.ride.RideSortFields
import com.wepool.app.data.model.enums.ride.RideSortFieldsWithOrder
import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User

object RideSortManager {

    fun sortRides(
        rides: List<Ride>,
        sortFields: List<RideSortFieldsWithOrder>,
        userMap: Map<String, User>,
        companyNameMap: Map<String, String>
    ): List<Ride> {
        return sortFields.fold(rides) { acc, sortFieldWithOrder ->
            val sorted = when (sortFieldWithOrder.field) {
                RideSortFields.DATE -> acc.sortedBy { it.date }
                RideSortFields.DEPARTURE_TIME -> acc.sortedBy { it.departureTime ?: "" }
                RideSortFields.ARRIVAL_TIME -> acc.sortedBy { it.arrivalTime ?: "" }
                RideSortFields.AVAILABLE_SEATS -> acc.sortedBy { it.availableSeats - it.occupiedSeats }
                RideSortFields.COMPANY_NAME -> acc.sortedBy { companyNameMap[it.companyCode] ?: "" }
                RideSortFields.USER_NAME -> acc.sortedBy { userMap[it.driverId]?.name ?: "" }
            }

            if (sortFieldWithOrder.order == SortOrder.DESCENDING) sorted.reversed() else sorted
        }
    }
}