package com.wepool.app.data.model.enums.ride

import com.wepool.app.data.model.enums.SortOrder

data class RideSortFieldsWithOrder(
    val field: RideSortFields,
    val order: SortOrder
)