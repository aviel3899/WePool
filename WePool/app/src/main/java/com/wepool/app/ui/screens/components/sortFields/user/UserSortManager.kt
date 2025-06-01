package com.wepool.app.ui.screens.components.sortFields.user

import com.wepool.app.data.model.enums.SortOrder
import com.wepool.app.data.model.enums.user.UserSortFieldWithOrder
import com.wepool.app.data.model.enums.user.UserSortFields
import com.wepool.app.data.model.users.User

object UserSortManager {

    fun sortUsers(
        users: List<User>,
        sortFields: List<UserSortFieldWithOrder>,
        companyMap: Map<String, String>
    ): List<User> {
        return sortFields.fold(users) { acc, sort ->
            when (sort.field) {
                UserSortFields.USER_NAME -> sortList(acc, sort.order) { it.name }
                UserSortFields.USER_EMAIL -> sortList(acc, sort.order) { it.email }
                UserSortFields.USER_PHONE -> sortList(acc, sort.order) { it.phoneNumber }
                UserSortFields.COMPANY_NAME -> sortList(acc, sort.order) { companyMap[it.companyCode] ?: "" }
            }
        }
    }

    private fun <T, R : Comparable<R>> sortList(
        list: List<T>,
        order: SortOrder,
        selector: (T) -> R
    ): List<T> {
        return when (order) {
            SortOrder.ASCENDING -> list.sortedBy(selector)
            SortOrder.DESCENDING -> list.sortedByDescending(selector)
        }
    }
}