package com.wepool.app.data.model.enums

enum class FilterField(val displayName: String) {
    COMPANY_NAME("Company Name"),
    USER_NAME("User Name"),
    DATE_RANGE("Date Range"),
    TIME_RANGE("Time Range"),
    DIRECTION("Direction");

    override fun toString(): String = displayName
}
