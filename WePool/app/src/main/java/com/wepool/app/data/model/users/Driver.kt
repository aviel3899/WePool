package com.wepool.app.data.model.users

import com.google.firebase.firestore.GeoPoint
import com.wepool.app.data.model.users.User
import com.wepool.app.data.model.enums.RideDirection

data class Driver(
    val user: User, // מידע בסיסי של המשתמש כולל roles
    val availableSeats: Int = 0, // מספר מקומות פנויים שהנהג מצהיר שיש לו
    val vehicleDetails: String = "", // תיאור הרכב לדוגמה: "Toyota Corolla 2019"
    val maxDetourMinutes: Int = 10, // סטייה מקסימלית (בדקות) מהמסלול שהנהג מוכן לבצע
    val preferredArrivalTime: String? = null, // שעת הגעה מועדפת למקום העבודה/הביתה (נקבעת ע"י הנהג)
    val calculatedDepartureTime: String? = null, // שעת יציאה שמחושבת אוטומטית לפי זמן ההגעה וה-API של גוגל
    val direction: RideDirection? = null, // האם הנסיעה היא TO_WORK או TO_HOME
    val destination: GeoPoint? = null, // מיקום היעד (מקום העבודה או כתובת בית)
    val activeRideId: String? = null // אם הנהג פרסם נסיעה – זה מזהה הנסיעה הפעילה
)
