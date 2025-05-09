package com.wepool.app.data.model.users

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val companyCode: String = "",
    val isBanned: Boolean = false,
    val isActive: Boolean = true,
    val roles: List<String> = emptyList(),
    val lastLoginTimestamp: Long? = null,
)
