package com.wepool.app.data.model.users

data class User(
    val uid: String,                        // Firebase UID – שדה חובה, לא מקבל default
    val name: String = "",                  // ברירת מחדל: מחרוזת ריקה
    val email: String = "",                 // ברירת מחדל: מחרוזת ריקה
    val phoneNumber: String = "",           // ברירת מחדל: מחרוזת ריקה
    val companyId: String? = null,          // יכול להיות null אם המשתמש לא שייך עדיין לארגון
    val isBanned: Boolean = false,          // ברירת מחדל: false
    val roles: List<String> = listOf()      // ברירת מחדל: רשימה ריקה (אין תפקידים עדיין)
)
