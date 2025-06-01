package com.wepool.app.data.model.users

import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.enums.user.UserSortFieldWithOrder

data class UserSearchFilters(
    val nameOrEmailOrPhone: String? = null,
    val companyCode: String? = null,
    val isActiveUser: Boolean? = null,
    val sortFields: List<UserSortFieldWithOrder> = emptyList(),
    val role: UserRole? = null
)
