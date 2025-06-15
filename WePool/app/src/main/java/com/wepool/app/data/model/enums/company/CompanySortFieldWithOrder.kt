package com.wepool.app.data.model.enums.company

import com.wepool.app.data.model.enums.SortOrder

data class CompanySortFieldWithOrder(
    val field: CompanySortFields,
    val order: SortOrder
)