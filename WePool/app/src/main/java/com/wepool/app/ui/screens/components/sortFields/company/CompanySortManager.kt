package com.wepool.app.ui.screens.components.sortFields.company

import com.wepool.app.data.model.company.Company
import com.wepool.app.data.model.enums.SortOrder
import com.wepool.app.data.model.enums.company.CompanySortFieldWithOrder
import com.wepool.app.data.model.enums.company.CompanySortFields

object CompanySortManager {

    fun sortCompanies(
        companies: List<Company>,
        sortFields: List<CompanySortFieldWithOrder>
    ): List<Company> {
        return sortFields.fold(companies) { acc, fieldWithOrder ->
            val comparator = when (fieldWithOrder.field) {
                CompanySortFields.COMPANY_NAME -> compareBy<Company> { it.companyName.lowercase() }
                CompanySortFields.CREATED_AT -> compareBy { it.createdAt }
            }

            if (fieldWithOrder.order == SortOrder.ASCENDING) {
                acc.sortedWith(comparator)
            } else {
                acc.sortedWith(comparator.reversed())
            }
        }
    }
}