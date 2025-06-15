package com.wepool.app.data.model.users

data class HRManager(
    val user: User = User(),
    val managedCompanyId: String = ""
)