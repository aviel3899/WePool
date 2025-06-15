package com.wepool.app.data.model.company

import com.google.firebase.Timestamp
import com.wepool.app.data.model.common.LocationData

data class Company(
    val companyId: String = "",
    val companyCode: String = "",
    val companyName: String = "",
    val location: LocationData? = null,
    val logoUrl: String? = null, // קישור ללוגו של החברה, לשימוש בעתיד בממשק משתמש
    val active: Boolean = true, // האם החברה פעילה
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null,
    val employees: List<String> = emptyList(),
    val hrManagerUid: String? = null
)