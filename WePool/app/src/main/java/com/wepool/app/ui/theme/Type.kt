package com.wepool.app.ui.theme

import androidx.compose.material3.Typography // שינוי ל-material3
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// הגדרת טיפוגרפיה בסיסית, ניתן להתאים אישית מאוחר יותר
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* ניתן להוסיף כאן סגנונות טקסט נוספים כמו:
    titleLarge = TextStyle( ... ),
    labelSmall = TextStyle( ... )
    ומאפיינים נוספים בהתאם לצרכים שלך
    */
)

