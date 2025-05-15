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

exports.sendNotificationToTokens = functions.https.onCall(async (data, context) => {
  try {
    console.log("📥 Received data keys:", Object.keys(data));

    const tokens = data?.data?.tokens;
    const title = data?.data?.title;
    const body = data?.data?.body;
    const rideId = data?.data?.rideId || "";

    console.log("📥 tokens =", tokens);
    console.log("📥 typeof tokens =", typeof tokens);
    console.log("📥 isArray =", Array.isArray(tokens));

   if (!Array.isArray(tokens)) {
     console.error("❌ tokens is not an array or missing:", tokens);
     throw new functions.https.HttpsError("invalid-argument", "tokens must be a non-empty array");
   }

   if (tokens.length === 0) {
     console.error("❌ tokens array is empty");
     throw new functions.https.HttpsError("invalid-argument", "tokens array cannot be empty");
   }

   if (!title || typeof title !== "string" || title.trim() === "") {
     console.error("❌ title is missing or invalid:", title);
     throw new functions.https.HttpsError("invalid-argument", "title must be a non-empty string");
   }

   if (!body || typeof body !== "string" || body.trim() === "") {
     console.error("❌ body is missing or invalid:", body);
     throw new functions.https.HttpsError("invalid-argument", "body must be a non-empty string");
   }

    console.log(`🔢 Tokens received: ${tokens.length}`);
    console.log(`📝 Title: ${title}`);
    console.log(`📝 Body: ${body}`);
    console.log(`🆔 Ride ID: ${rideId}`);

    const message = {
      notification: {
        title: title,
        body: body
      },
      android: {
        notification: {
          icon: "ic_launcher_foreground"
        }
      },
      data: {
        type: "passenger_notification",
        rideId: rideId
      },
      tokens: tokens
    };

    const response = await admin.messaging().sendEachForMulticast(message);
    console.log("📬 Multicast summary:", response.successCount, "sent,", response.failureCount, "failed");

    const results = await Promise.all(response.responses.map(async (res, index) => {
      const token = tokens[index];

      if (res.success) {
        console.log(`✅ Notification sent to token: ${token}`);
        return { token, success: true };
      } else {
        const error = res.error;
        console.error(`❌ Failed to send to token: ${token}`, error);

        // Optionally remove invalid tokens from DB (if needed)
        if (
          error.code === 'messaging/registration-token-not-registered' ||
          error.code === 'messaging/invalid-argument'
        ) {
          try {
            // You can optionally delete token from wherever you store it
            console.warn(`🧹 Token invalid: ${token} — remove manually if needed.`);
          } catch (deletionError) {
            console.error(`❌ Error removing token: ${token}`, deletionError);
          }
        }

        return { token, success: false, error: error.message };
      }
    }));

    return { success: true, results };

  } catch (err) {
    console.error("❌ Unexpected error in sendNotificationToTokens:", err);
    throw new functions.https.HttpsError("internal", err.message || "Unknown error");
  }
});

