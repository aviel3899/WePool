package com.wepool.app.data.model.users

import com.wepool.app.data.model.common.LocationData
import com.wepool.app.data.model.enums.user.UserRole

@kotlinx.serialization.Serializable
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val companyCode: String = "",
    val banned: Boolean = false,
    val active: Boolean = false,
    val roles: List<UserRole> = emptyList(),
    val lastLoginTimestamp: Long? = null,
    val fcmToken: String? = null,
    val favoriteLocations: List<LocationData> = emptyList()
)
