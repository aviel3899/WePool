/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

// import {onRequest} from "firebase-functions/v2/https";
// import * as logger from "firebase-functions/logger";

// Start writing functions
// https://firebase.google.com/docs/functions/typescript

// export const helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

import * as functions from "firebase-functions";
// מומלץ להשתמש בלוגר החדש יותר v2
import * as logger from "firebase-functions/logger";
// ייבוא ספריית הלקוח של Google Maps
import { Client, DirectionsRequest } from "@googlemaps/google-maps-services-js";

// אתחול הלקוח של Google Maps
// שים לב: מפתח ה-API מתקבל מהקונפיגורציה בזמן ריצה, לא בהכרח בעת יצירת הלקוח
const mapsClient = new Client({});

// (אופציונלי) הגדרת טיפוסים עבור הנתונים שהפונקציה מקבלת ומחזירה
interface RouteDataRequest {
    origin: string;
    destination: string;
    // אפשר להוסיף שדות נוספים שהלקוח שולח
}

interface RouteDataResponse {
    routeInfo: any; // עדיף להגדיר טיפוס מדויק יותר בהתאם לתשובת ה-API
}

// דוגמה לפונקציה מסוג Callable Function (מופעלת ישירות מהאפליקציה)
export const getRouteData = functions.https.onCall(
    async (data: RouteDataRequest, context): Promise<RouteDataResponse> => {
        // קבלת מפתח ה-API מקונפיגורציית הפונקציות
        // ודא שהרצת: firebase functions:config:set maps.key="..."
        const apiKey = functions.config().maps.key as string; // המרה לטיפוס string

        if (!apiKey) {
            logger.error("Maps API key not configured in functions config (maps.key)");
            throw new functions.https.HttpsError('internal', 'Server configuration error.');
        }

        // (אופציונלי) בדיקה אם המשתמש מאומת
        // if (!context.auth) {
        //     logger.error("User is not authenticated.");
        //     throw new functions.https.HttpsError('unauthenticated', 'The function must be called while authenticated.');
        // }

        const { origin, destination } = data;

        // הכנת הפרמטרים לבקשת ה-Directions API
        // שימוש ב-Partial מאפשר לשלוח רק חלק מהשדות האפשריים
        const params: Partial<DirectionsRequest['params']> = {
            origin: origin,
            destination: destination,
            key: apiKey, // <-- שימוש במפתח מה-config!
        };

        try {
            logger.info(`Requesting directions from ${origin} to ${destination}`);
            // קריאה ל-API של Directions
            const response = await mapsClient.directions({
              params: params as DirectionsRequest['params'], // המרה לטיפוס המלא במידת הצורך
              timeout: 5000, // milliseconds
            });

            logger.info(`Directions API response status: ${response.data.status}`);

            // עיבוד התשובה והחזרת המידע הרלוונטי לאפליקציה
            // למשל: const duration = response.data.routes[0]?.legs[0]?.duration.text;
            return { routeInfo: response.data }; // החזר רק מה שהאפליקציה צריכה

        } catch (error: any) { // טיפול בשגיאות עם הגדרת טיפוס
            logger.error("Error calling Directions API:", error);
            // אפשר להוסיף טיפול ספציפי יותר בשגיאות מה-API של גוגל
            if (error.response) {
                logger.error("Google Maps API Error:", error.response.data);
            }
            throw new functions.https.HttpsError('internal', 'Failed to get directions.');
        }
    }
);

// פונקציית דוגמה נוספת (נוצרת לעיתים על ידי init)
export const helloWorld = functions.https.onRequest((request, response) => {
  logger.info("Hello logs!", {structuredData: true});
  response.send("Hello from Firebase!");
});

// כאן תוסיף את שאר הפונקציות של פרויקט WePool