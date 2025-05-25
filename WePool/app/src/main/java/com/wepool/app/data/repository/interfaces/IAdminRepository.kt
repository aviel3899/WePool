package com.wepool.app.data.repository.interfaces

import com.wepool.app.data.model.users.Admin
import com.wepool.app.data.model.users.User

interface IAdminRepository {
    suspend fun isAdmin(user: User): Boolean
    suspend fun getAdmin(user: User): Admin?
}
