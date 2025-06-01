package com.wepool.app.data.repository

import android.util.Log
import com.wepool.app.data.model.enums.user.UserRole
import com.wepool.app.data.model.users.HRManager
import com.wepool.app.data.model.users.User
import com.wepool.app.data.repository.interfaces.IAdminRepository
import com.wepool.app.data.repository.interfaces.ICompanyRepository
import com.wepool.app.data.repository.interfaces.IHRManagerRepository
import com.wepool.app.data.repository.interfaces.IUserRepository

class AdminRepository(
    private val userRepository: IUserRepository,
    private val hrManagerRepository: IHRManagerRepository,
    private val companyRepository: ICompanyRepository
) : IAdminRepository {

    override suspend fun isAdmin(user: User): Boolean {
        val email = user.email.trim().lowercase()
        return com.wepool.app.infrastructure.config.AdminConfig.authorizedAdminEmails.contains(email)
    }

    override suspend fun getAdmin(user: User): com.wepool.app.data.model.users.Admin? {
        return if (isAdmin(user)) com.wepool.app.data.model.users.Admin(user) else null
    }

}
