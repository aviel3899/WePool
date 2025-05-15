/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

// ייבוא הספריות הנדרשות
const functions = require("firebase-functions"); // הליבה של Cloud Functions
const admin = require("firebase-admin"); // (אופציונלי) אם צריך גישה ל-Firestore/Auth מצד השרת

// אם אתה צריך את ה-Admin SDK (למשל, גישה ישירה ל-Firestore), בטל את ההערה הבאה:
admin.initializeApp();

// ייבוא ספריית הלקוח של Google Maps
// ודא שהתקנת אותה! הרץ: npm install @googlemaps/google-maps-services-js
// בתוך תיקיית functions
const { Client } = require("@googlemaps/google-maps-services-js");
const mapsClient = new Client({}); // אתחול הלקוח

/**
 * פונקציה מסוג Callable - לקבלת נתוני מסלול מ-Google Directions API.
 * מופעלת ישירות מאפליקציית הלקוח.
 * מצפה לקבל אובייקט data עם המפתחות origin ו-destination.
 * מחזירה אובייקט עם המפתח routeInfo המכיל את תשובת ה-API.
 */
exports.getRouteData = functions.https.onCall(async (data, context) => {

  // ולידציה בסיסית של הקלט
  if (!data || !data.origin || !data.destination) {
    functions.logger.error(
      "Missing origin or destination in request data.",
      data
    );
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The function must be called with 'origin' and 'destination' arguments."
    );
  }

  const { origin, destination } = data;

  // קבלת מפתח ה-API מקונפיגורציית הפונקציות
  // ודא שהרצת: firebase functions:config:set maps.key="..."
  // שימוש ב-optional chaining ?. למקרה שההגדרה לא קיימת
  const apiKey = functions.config().maps?.key;

  if (!apiKey) {
    functions.logger.error(
      "Maps API key not configured in functions config (maps.key)."
    );
    throw new functions.https.HttpsError(
      "internal",
      "Server configuration error."
    );
  }

  // הכנת הפרמטרים לבקשת ה-Directions API
  const params = {
    origin: origin,
    destination: destination,
    key: apiKey, // שימוש במפתח מה-config
  };

  try {
    functions.logger.info(
      `Requesting directions from ${origin} to ${destination}`
    );
    // קריאה ל-API של Directions
    const response = await mapsClient.directions({
      params: params,
      timeout: 5000, // milliseconds
    });

    functions.logger.info(
      `Directions API response status: ${response.data.status}`
    );

    // החזרת המידע הרלוונטי לאפליקציית הלקוח
    // כאן אפשר לעבד את התשובה לפני השליחה אם רוצים
    return { routeInfo: response.data };
  } catch (error) {
    functions.logger.error("Error calling Directions API:", error);
    // הדפסת שגיאה ספציפית מה-API של גוגל אם קיימת
    if (error.response) {
      functions.logger.error("Google Maps API Error:", error.response.data);
    }
    // זריקת שגיאה שהלקוח יקבל
    throw new functions.https.HttpsError(
      "internal",
      "Failed to get directions."
    );
  }
});


exports.sendNotificationToPassengers = functions.https.onCall(async (data, context) => {
  try {
    console.log("📥 Received data keys:", Object.keys(data));
    const rideId = data?.data?.rideId;
    console.log("🔎 rideId extracted:", rideId);

    if (!rideId) {
      console.error("❌ Missing rideId in request payload");
      throw new functions.https.HttpsError("invalid-argument", "Missing rideId");
    }

    const rideDoc = await admin.firestore().collection("rides").doc(rideId).get();
    if (!rideDoc.exists) {
      console.error(`❌ Ride not found in Firestore: ${rideId}`);
      throw new functions.https.HttpsError("not-found", `Ride not found: ${rideId}`);
    }

    const rideData = rideDoc.data();
    const pickupStops = Array.isArray(rideData.pickupStops) ? rideData.pickupStops : [];

    const passengerIds = pickupStops
      .map(stop => stop.passengerId)
      .filter(uid => typeof uid === "string" && uid.length > 0);

    const tokenList = [];

    await Promise.all(passengerIds.map(async (uid) => {
      const userDoc = await admin.firestore().collection("users").doc(uid).get();
      if (!userDoc.exists) return;

      const userData = userDoc.data();
      const token = userData?.fcmToken;

      console.log(`🔎 token for UID ${uid}:`, token);

      if (token) {
        tokenList.push({ uid, token });
      }
    }));

    if (tokenList.length === 0) {
      console.warn("⚠️ No valid tokens found for passengers");
      return { success: false, message: "No tokens found" };
    }

    const tokens = tokenList.map(entry => entry.token);

    const message = {
      notification: {
        title: "WePool",
        body: "🚗 הנהג התחיל את הנסיעה",
      },
      android: {
          notification: {
            icon: "ic_launcher_foreground"
          }
        },
      data: {
        type: "ride_start",
        rideId: rideId,
      },
      tokens: tokens
    };

    const response = await admin.messaging().sendEachForMulticast(message);
    console.log("📬 Multicast response summary:", response.successCount, "sent,", response.failureCount, "failed");

    const results = await Promise.all(response.responses.map(async (res, index) => {
      const { uid } = tokenList[index];

      if (res.success) {
        console.log(`✅ Notification sent to UID: ${uid}`);
        return { uid, success: true };
      } else {
        const error = res.error;
        console.error(`❌ Failed to send to UID: ${uid}`, error);

        if (
          error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/invalid-argument'
        ) {
          try {
            await admin.firestore().collection("users").doc(uid).update({
              fcmToken: admin.firestore.FieldValue.delete()
            });
            console.warn(`🧹 טוקן לא תקף נמחק עבור UID: ${uid}`);
          } catch (deletionError) {
            console.error(`❌ שגיאה במחיקת fcmToken עבור UID: ${uid}`, deletionError);
          }
        }

        return { uid, success: false, error: error.message };
      }
    }));

    return { success: true, results };
  } catch (err) {
    console.error("❌ Unexpected error in sendNotificationToPassengers:", err);
    throw new functions.https.HttpsError("internal", err.message || "Unknown error");
  }
});


exports.sendNotificationToPassenger = functions.https.onCall(async (data, context) => {
  try {
    const { passengerId, title, body, rideId } = data || {};
    if (!passengerId || !title || !body) {
      throw new functions.https.HttpsError("invalid-argument", "Missing required fields");
    }

    const userDoc = await admin.firestore().collection("users").doc(passengerId).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", `User not found: ${passengerId}`);
    }

    const userData = userDoc.data();
    const token = userData?.fcmToken;

    if (!token) {
      throw new functions.https.HttpsError("not-found", `No FCM token for user: ${passengerId}`);
    }

    const message = {
      notification: {
        title,
        body
      },
      data: {
        type: "passenger_notification",
        rideId: rideId || ""
      },
      token
    };

    const response = await admin.messaging().send(message);
    console.log(`📬 Notification sent to ${passengerId}:`, response);
    return { success: true };

  } catch (err) {
    console.error("❌ Failed to send passenger notification:", err);
    throw new functions.https.HttpsError("internal", err.message || "Unknown error");
  }
});

