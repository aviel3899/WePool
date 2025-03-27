-- יצירת טבלאות בסיס לפי ה-ERD המעודכן
-- 1. טבלת Company
CREATE TABLE Company (
    companyId SERIAL PRIMARY KEY,
    companyName TEXT NOT NULL
);

-- 2. טבלת User
CREATE TABLE Users (
    userId SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    companyId INT REFERENCES Company(companyId),
    isBanned BOOLEAN DEFAULT FALSE
);

ALTER TABLE Users
DROP PASSWORD;

SELECT * FROM Users;

ALTER TABLE Users
DROP CONSTRAINT IF EXISTS Users_companyId_fkey;

-- אם חברה נמחקת, המשתמשים לא יימחקו רק companyId שלהם יהפוך לNULL
ALTER TABLE Users
ADD CONSTRAINT fk_users_company
FOREIGN KEY (companyId) REFERENCES Company(companyId) ON DELETE SET NULL;

ALTER TABLE Users
ADD COLUMN firebaseUid VARCHAR(128) UNIQUE NOT NULL;

-- 3. טבלאות לפי תפקיד
CREATE TABLE Driver (
    userId INT PRIMARY KEY REFERENCES Users(userId),
    availableSeats INT NOT NULL,
    vehicleDetails TEXT,
    maxDetourMinutes INT DEFAULT 10
);

ALTER TABLE Driver
DROP CONSTRAINT IF EXISTS driver_userid_fkey;

ALTER TABLE Driver
ADD CONSTRAINT fk_driver_user
FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE;

CREATE EXTENSION postgis;

CREATE TABLE Passenger (
    userId INT PRIMARY KEY REFERENCES Users(userId),
    preferredPickupLocation GEOGRAPHY(Point, 4326)
);

ALTER TABLE Passenger
DROP CONSTRAINT IF EXISTS passenger_userid_fkey;

ALTER TABLE Passenger
ADD CONSTRAINT fk_passenger_user
FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE;

CREATE TABLE HR_Manager (
    userId INT PRIMARY KEY REFERENCES Users(userId),
    maxBadPoints INT DEFAULT 5
);

ALTER TABLE HR_Manager
DROP CONSTRAINT IF EXISTS hr_manager_userid_fkey;

ALTER TABLE HR_Manager
ADD CONSTRAINT fk_hr_user
FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE;

CREATE TABLE Admin (
    userId INT PRIMARY KEY REFERENCES Users(userId)
);

ALTER TABLE Admin
DROP CONSTRAINT IF EXISTS admin_userid_fkey;

ALTER TABLE Admin
ADD CONSTRAINT fk_admin_user
FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE;

-- אם משתמש נמחק, התפקיד שלו גם יימחק אוטומטית

-- 4. טבלת הגדרות
CREATE TABLE Settings (
    settingId SERIAL PRIMARY KEY,
    companyId INT REFERENCES Company(companyId),
    key TEXT NOT NULL,
    value TEXT NOT NULL
);

ALTER TABLE Settings
DROP CONSTRAINT IF EXISTS settings_companyid_fkey;

ALTER TABLE Settings
ADD CONSTRAINT fk_settings_company
FOREIGN KEY (companyId) REFERENCES Company(companyId) ON DELETE CASCADE;

--כשחברה נמחקת, גם ההגדרות הכלליות שלה נמחקות — אין להן יותר שימוש.

-- 5. טבלת RouteTemplate
CREATE TABLE RouteTemplate (
    templateId SERIAL PRIMARY KEY,
    origin GEOGRAPHY(Point, 4326) NOT NULL,
    destination GEOGRAPHY(Point, 4326) NOT NULL,
    boundingBox GEOMETRY(POLYGON, 4326),
    polyline TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. טבלת Ride
CREATE TABLE Ride (
    rideId SERIAL PRIMARY KEY,
    driverId INT REFERENCES Driver(userId),
    routeTemplateId INT REFERENCES RouteTemplate(templateId),
    startLocation GEOGRAPHY(Point, 4326) NOT NULL,
    destination GEOGRAPHY(Point, 4326) NOT NULL,
    departureTime TIMESTAMP NOT NULL,
    arrivalTime TIMESTAMP NOT NULL,
    maxDetourMinutes INT DEFAULT 10,
    availableSeats INT NOT NULL,
    status TEXT DEFAULT 'planned',
    etaWithoutPassenger INT
);

ALTER TABLE Ride
DROP CONSTRAINT IF EXISTS ride_driverid_fkey;

ALTER TABLE Ride
ADD CONSTRAINT fk_ride_driver
FOREIGN KEY (driverId) REFERENCES Driver(userId) ON DELETE SET NULL;
-- אם נהג נמחק, נשאיר את רשומת הנסיעה לצרכים היסטוריים אבל ננתק ממנה את הקשר לנהג.

ALTER TABLE Ride
DROP CONSTRAINT IF EXISTS ride_routetemplateid_fkey;

ALTER TABLE Ride
ADD CONSTRAINT fk_ride_template
FOREIGN KEY (routeTemplateId) REFERENCES RouteTemplate(templateId) ON DELETE SET NULL;
--אם מסלול טיפוסי נמחק, הנסיעה תישאר תקפה, אך תוצג עם נתוני המסלול הישירים.

-- 7. טבלת RidePassenger
CREATE TABLE RidePassenger (
    rideId INT REFERENCES Ride(rideId),
    passengerId INT REFERENCES Passenger(userId),
    pickupLocation GEOGRAPHY(Point, 4326),
    detourTimeSeconds INT,
    status TEXT DEFAULT 'pending',
    PRIMARY KEY (rideId, passengerId)
);

ALTER TABLE RidePassenger
DROP CONSTRAINT IF EXISTS ridepassenger_rideid_fkey;

ALTER TABLE RidePassenger
DROP CONSTRAINT IF EXISTS ridepassenger_passengerid_fkey;

ALTER TABLE RidePassenger
ADD CONSTRAINT fk_ridepassenger_ride
FOREIGN KEY (rideId) REFERENCES Ride(rideId) ON DELETE CASCADE;

ALTER TABLE RidePassenger
ADD CONSTRAINT fk_ridepassenger_passenger
FOREIGN KEY (passengerId) REFERENCES Passenger(userId) ON DELETE CASCADE;

--אם נסיעה או נוסע נמחקים, גם החיבור ביניהם יימחק אוטומטית

-- 8. טבלת RideHistory
CREATE TABLE RideHistory (
    historyId SERIAL PRIMARY KEY,
    userId INT REFERENCES Users(userId),
    rideId INT REFERENCES Ride(rideId),
    status TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE RideHistory
DROP CONSTRAINT IF EXISTS ridehistory_userid_fkey;

ALTER TABLE RideHistory
DROP CONSTRAINT IF EXISTS ridehistory_rideid_fkey;

ALTER TABLE RideHistory
ADD CONSTRAINT fk_ridehistory_user
FOREIGN KEY (userId) REFERENCES Users(userId) ON DELETE CASCADE;

ALTER TABLE RideHistory
ADD CONSTRAINT fk_ridehistory_ride
FOREIGN KEY (rideId) REFERENCES Ride(rideId) ON DELETE CASCADE;

--אם נמחק משתמש או נסיעה, נרצה שהיסטוריית הנסיעות תימחק בהתאם (לא לשמור על היסטוריה שקשורה לנתונים שנמחקו).

-- אינדקסים מרחביים (לשיפור ביצועים)
CREATE INDEX idx_passenger_location ON Passenger USING GIST (preferredPickupLocation);
CREATE INDEX idx_ride_start_location ON Ride USING GIST (startLocation);
CREATE INDEX idx_ride_dest_location ON Ride USING GIST (destination);
CREATE INDEX idx_template_box ON RouteTemplate USING GIST (boundingBox);