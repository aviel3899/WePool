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

exports.sendRideStartedNotification = functions.https.onCall(async (data, context) => {
  const rideId = data.rideId;
  if (!rideId) {
    console.error("❌ rideId is missing in the request");
    throw new functions.https.HttpsError("invalid-argument", "Missing rideId");
  }

  try {
    const rideDoc = await admin.firestore().collection("rides").doc(rideId).get();
    if (!rideDoc.exists) {
      console.error(`❌ Ride not found for rideId=${rideId}`);
      throw new functions.https.HttpsError("not-found", "Ride not found");
    }

    const rideData = rideDoc.data();
    const pickupStops = rideData.pickupStops || [];

    if (!Array.isArray(pickupStops)) {
      console.error(`❌ pickupStops is not an array for rideId=${rideId}`);
      throw new functions.https.HttpsError("data-loss", "pickupStops must be an array");
    }

    const tokens = [];

    for (const stop of pickupStops) {
      const passengerId = stop.passengerId;
      if (!passengerId) {
        console.warn(`⚠️ Missing passengerId in one of the pickupStops for rideId=${rideId}`);
        continue;
      }

      const userDoc = await admin.firestore().collection("users").doc(passengerId).get();
      if (!userDoc.exists) {
        console.warn(`⚠️ No user found with UID=${passengerId}`);
        continue;
      }

      const userData = userDoc.data();
      const fcmToken = userData.fcmToken;

      if (!fcmToken) {
        console.warn(`⚠️ No FCM token found for passenger UID=${passengerId}`);
        continue;
      }

      tokens.push(fcmToken);
      console.log(`✅ Added FCM token for passenger UID=${passengerId}`);
    }

    if (tokens.length === 0) {
      console.warn(`⚠️ No valid tokens found for rideId=${rideId}`);
      return { success: false, message: "No valid FCM tokens found" };
    }

    const payload = {
      notification: {
        title: "הנסיעה שלך החלה!",
        body: "הנהג התחיל את הניווט. היכנס כדי לעקוב אחרי הנסיעה.",
      },
      data: {
        rideId: rideId,
      },
    };

    const response = await admin.messaging().sendToDevice(tokens, payload);
    console.log(`📤 נשלחו ${tokens.length} התראות.`, response);

    return { success: true, message: "Notifications sent" };

  } catch (error) {
    console.error("❌ שגיאה כללית בשליחת התראות:", error);
    throw new functions.https.HttpsError("internal", "Failed to send notifications");
  }
});


exports.sendNotificationToPhoneNumber = functions.https.onCall(async (data, context) => {
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

  const phoneToTokenMap = new Map();

  await Promise.all(passengerIds.map(async (uid) => {
    const userDoc = await admin.firestore().collection("users").doc(uid).get();
    if (!userDoc.exists) return;

    const userData = userDoc.data();
    const phoneNumber = userData?.phoneNumber;
    const token = userData?.fcmToken;

    if (phoneNumber && token && phoneNumber.startsWith("+972")) {
        phoneToTokenMap.set(phoneNumber, token)
    }
  }));

  if (phoneToTokenMap.size === 0) {
    console.warn("⚠️ No phone/token pairs found for passengers");
    return { success: false, message: "No phone/token pairs found" };
  }

  const payload = {
    notification: {
      title: "🚗 הנהג התחיל את הנסיעה",
      body: "אנא היערך לנקודת האיסוף",
    },
    data: {
      type: "ride_start",
      rideId: rideId,
    },
  };

  const results = [];

  for (const [phone, token] of phoneToTokenMap.entries()) {
    try {
      const response = await admin.messaging().sendToDevice(token, payload);
      console.log(`📱 Notification sent to ${phone}`, response);
      results.push({ phone, success: true });
    } catch (error) {
      console.error(`❌ Failed to send to ${phone}`, error);
      results.push({ phone, success: false, error: error.message });
    }
  }
  return { success: true, results };
  } catch (err) {
      console.error("❌ Unexpected error in sendNotificationToPhoneNumber:", err);
      throw new functions.https.HttpsError("internal", err.message || "Unknown error");
    }
});
