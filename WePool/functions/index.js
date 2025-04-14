/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

// const {onRequest} = require("firebase-functions/v2/https");
// const logger = require("firebase-functions/logger");

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

// ייבוא הספריות הנדרשות
const functions = require("firebase-functions"); // הליבה של Cloud Functions
const admin = require("firebase-admin"); // (אופציונלי) אם צריך גישה ל-Firestore/Auth מצד השרת

// אם אתה צריך את ה-Admin SDK (למשל, גישה ישירה ל-Firestore), בטל את ההערה הבאה:
// admin.initializeApp();

// ייבוא ספריית הלקוח של Google Maps
// ודא שהתקנת אותה! הרץ: npm install @googlemaps/google-maps-services-js
// בתוך תיקיית functions
const { Client } = require("@googlemaps/google-maps-services-js");
const mapsClient = new Client({}); // אתחול הלקוח

// ------------------- פונקציות לדוגמה -------------------

/**
 * פונקציית HTTPS פשוטה לבדיקת הפריסה.
 * ניתן לגשת אליה דרך URL.
 */
exports.helloWorld = functions.https.onRequest((request, response) => {
  // שימוש בלוגר המובנה של Firebase Functions
  functions.logger.info("Hello World function called!", { structuredData: true });
  response.send("Hello from WePool Firebase!");
});

/**
 * פונקציה מסוג Callable - לקבלת נתוני מסלול מ-Google Directions API.
 * מופעלת ישירות מאפליקציית הלקוח.
 * מצפה לקבל אובייקט data עם המפתחות origin ו-destination.
 * מחזירה אובייקט עם המפתח routeInfo המכיל את תשובת ה-API.
 */
exports.getRouteData = functions.https.onCall(async (data, context) => {
  // (אופציונלי) בדיקה אם המשתמש מאומת
  // if (!context.auth) {
  //   functions.logger.error("User is not authenticated for getRouteData.");
  //   // זריקת שגיאה שהלקוח יקבל
  //   throw new functions.https.HttpsError(
  //     "unauthenticated",
  //     "The function must be called while authenticated."
  //   );
  // }

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

// ------------------- הוסף כאן פונקציות נוספות -------------------
// לדוגמה, פונקציות שמופעלות על ידי אירועים ב-Firestore (triggers),
// פונקציות אימות, וכו'.