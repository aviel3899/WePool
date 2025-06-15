package com.wepool.app.data.model.enums.user

import com.wepool.app.data.model.enums.SortOrder

data class UserSortFieldWithOrder(
    val field: UserSortFields,
    val order: SortOrder
)