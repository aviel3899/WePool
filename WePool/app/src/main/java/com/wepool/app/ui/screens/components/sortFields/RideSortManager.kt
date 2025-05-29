package com.wepool.app.ui.screens.components.sortFields

import com.wepool.app.data.model.ride.Ride
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.enums.SortFields

object RideSortManager {

    fun sortRides(
        rides: List<Ride>,
        sortFields: List<SortFields>,
        userMap: Map<String, User>,
        companyNameMap: Map<String, String>
    ): List<Ride> {
        return sortFields.fold(rides) { acc, field ->
            when (field) {
                SortFields.DATE -> acc.sortedBy { it.date }
                SortFields.DEPARTURE_TIME -> acc.sortedBy { it.departureTime ?: "" }
                SortFields.ARRIVAL_TIME -> acc.sortedBy { it.arrivalTime ?: "" }
                SortFields.AVAILABLE_SEATS -> acc.sortedBy { it.availableSeats - it.occupiedSeats }
                SortFields.COMPANY_NAME -> acc.sortedBy {
                    companyNameMap[it.companyCode] ?: ""
                }
                SortFields.USER -> acc.sortedBy {
                    userMap[it.driverId]?.name ?: ""
                }
            }
        }
    }
}
