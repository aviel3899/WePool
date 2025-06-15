package com.wepool.app.data.model.users

import com.wepool.app.data.model.enums.user.UserRole

data class RoleOnlyFilters(
    val role: UserRole? = null
)