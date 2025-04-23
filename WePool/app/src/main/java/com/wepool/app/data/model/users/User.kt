package com.wepool.app.data.model.users

import com.wepool.app.data.model.enums.UserRole

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val companyId: String = "",
    val isBanned: Boolean = false,
    val isActive: Boolean = true,
    val roles: List<String> = emptyList()
)
