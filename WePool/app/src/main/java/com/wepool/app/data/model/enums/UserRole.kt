package com.wepool.app.data.model.enums

@kotlinx.serialization.Serializable
enum class UserRole {
    ADMIN,
    DRIVER,
    PASSENGER,
    HR_MANAGER
}