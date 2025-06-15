package com.wepool.app.data.model.enums.ride

enum class RideFilterFields(val displayName: String) {
    COMPANY_NAME("Company Name"),
    USER_NAME("User Name"),
    PHONE("Phone"),
    DATE_RANGE("Date Range"),
    TIME_RANGE("Time Range"),
    DIRECTION("Direction");

    override fun toString(): String = displayName
}