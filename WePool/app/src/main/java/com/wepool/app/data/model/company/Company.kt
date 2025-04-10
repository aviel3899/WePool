package com.wepool.app.data.model.company

data class Company(
    val companyId: String,                  // מזהה החברה – חובה
    val companyName: String = ""            // ברירת מחדל: מחרוזת ריקה
)