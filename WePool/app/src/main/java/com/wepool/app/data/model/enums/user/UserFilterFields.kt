package com.wepool.app.data.model.enums.user

enum class UserFilterFields(val displayName: String) {
    USER_NAME("User Name"),
    PHONE("Phone"),
    COMPANY_NAME("Company Name"),
    ACTIVE_USER("Status"),
    USER_ROLE("Role");

    override fun toString(): String = displayName
}