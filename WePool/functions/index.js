// 🔹 Firebase Admin SDK
const admin = require("firebase-admin");
admin.initializeApp();

// 🔹 Google Maps Client
const { Client } = require("@googlemaps/google-maps-services-js");
const mapsClient = new Client({});

// 🔹 Firebase Functions v2 API
const { onCall, defineSecret, HttpsError } = require("firebase-functions/v2/https");

// 🔐 Secret (מוגדר ע"י CLI: firebase functions:secrets:set MAPS_API_KEY)
const mapsApiKey = defineSecret("MAPS_API_KEY");

// 🔹 פונקציה לקבלת נתוני מסלול מ-Google Directions API
exports.getRouteData = onCall({ secrets: [mapsApiKey] }, async (request) => {
  const data = request.data;
  const origin = data?.origin;
  const destination = data?.destination;

  if (!origin || !destination) {
    throw new HttpsError("invalid-argument", "Missing 'origin' or 'destination'.");
  }

  try {
    const response = await mapsClient.directions({
      params: {
        origin,
        destination,
        key: process.env.MAPS_API_KEY,
      },
      timeout: 5000,
    });

    return { routeInfo: response.data };
  } catch (error) {
    console.error("❌ Directions API error:", error);
    throw new HttpsError("internal", "Failed to fetch route data.");
  }
});

// 🔹 פונקציה לשליחת התראה לנוסעים על תחילת נסיעה
exports.sendNotificationToPassengers = onCall({ secrets: [mapsApiKey] }, async (request) => {
  const rideId = request.data?.rideId;

  if (!rideId) {
    throw new HttpsError("invalid-argument", "Missing rideId");
  }

  const rideDoc = await admin.firestore().collection("rides").doc(rideId).get();
  if (!rideDoc.exists) {
    throw new HttpsError("not-found", `Ride not found: ${rideId}`);
  }

  const rideData = rideDoc.data();
  const pickupStops = Array.isArray(rideData.pickupStops) ? rideData.pickupStops : [];

  const passengerIds = pickupStops
    .map((stop) => stop.passengerId)
    .filter((id) => typeof id === "string" && id.length > 0);

  const tokens = [];
  await Promise.all(passengerIds.map(async (uid) => {
    const userDoc = await admin.firestore().collection("users").doc(uid).get();
    if (!userDoc.exists) return;
    const token = userDoc.data()?.fcmToken;
    if (token) tokens.push(token);
  }));

  if (tokens.length === 0) {
    return { success: false, message: "No FCM tokens found" };
  }

  const payload = {
    notification: {
      title: "🚗 הנהג התחיל את הנסיעה",
      body: "אנא היערך לנקודת האיסוף",
    },
    data: {
      type: "ride_start",
      rideId,
    },
  };

  await admin.messaging().sendToDevice(tokens, payload);
  return { success: true, message: "Notifications sent" };
});
