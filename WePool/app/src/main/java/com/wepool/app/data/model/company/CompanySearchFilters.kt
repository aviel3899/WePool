package com.wepool.app.data.model.company

import com.wepool.app.data.model.enums.company.CompanySortFieldWithOrder

data class CompanySearchFilters(
    val companyName: String? = null,
    val sortFields: List<CompanySortFieldWithOrder> = emptyList()
)