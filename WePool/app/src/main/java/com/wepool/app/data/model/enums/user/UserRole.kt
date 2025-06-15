package com.wepool.app.data.model.enums.user

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    ADMIN,
    DRIVER,
    PASSENGER,
    HR_MANAGER,
    All
}