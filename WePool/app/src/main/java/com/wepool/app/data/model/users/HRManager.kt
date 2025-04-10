package com.wepool.app.data.model.users

data class HRManager(
    val user: User,                             // פרטי המשתמש
    val managedCompanyId: String = ""           // מזהה החברה שה-HR אחראי עליה
)
