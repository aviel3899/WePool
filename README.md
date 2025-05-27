**WePool 🚗**

WePool is a smart internal carpool management application designed for organizations, developed as a graduation project at Afeka College of Engineering.

The app helps employees of the same company coordinate safe, efficient, and eco-friendly shared rides. With an intelligent ride-matching engine and deep integration with Google Maps and Firebase, WePool improves commuting experiences while promoting sustainability and trust within the workplace.

**🚀 Features**

  🔐 Role-based access system: Driver, Passenger, HR Manager, Admin.

  🧠 Intelligent ride-matching:

      Based on arrival time, route detour tolerance, and available seats

      Real-time detour calculation using Google Directions API

  📍 Maps & Navigation Integration:

      Address autocomplete (Google Places API)

      Route encoding/decoding and optimization

      Real-time pickup tracking and live navigation via Google Maps

  🔔 Push notifications:

      Firebase Cloud Messaging (FCM) integration

      Alerts for ride status, approvals, and pickups

  🧭 Live ride management:

      Ride creation, join requests, passenger approvals, cancellations

      Auto-update of pickup/drop-off times when passengers are added or removed

  🛡️ Secure authentication:

      Firebase Email/Password Auth

      Optional SMS verification via Firebase Phone Auth

  📱 Modern UI:

      Built entirely with Jetpack Compose

      Responsive design and dynamic component rendering

  🗃️ Full history and admin views:

      Ride history per user

      Company management and HR assignment (admin-only)


**🛠️ Built With**

| 🛠️ Technology               | 💡 Purpose                                 |
|-----------------------------|--------------------------------------------|
| Kotlin + Jetpack Compose   | Android development & declarative UI       |
| Firebase Firestore         | Cloud NoSQL database                       |
| Firebase Authentication    | User login and security                    |
| Firebase Cloud Functions   | Backend business logic & notifications     |
| Firebase Cloud Messaging   | Push notifications                         |
| Firebase Storage           | Static map images and attachments          |
| Google Maps SDK            | Map views, Directions, Navigation          |
| Google Places API          | Address autocomplete & place details       |
| Git & GitHub               | Version control and collaboration          |


**📁 Project Structure**

    wepool/
    🗂️ data/                  # Models, enums, repositories
    🗂️ infrastructure/        # Firebase providers and setup
    🗂️ services/              # Location, FCM, Navigation services
    🗂️ ui/                    # All Composable screens & UI components
       🔹 driverScreens/
       🔹 passengerScreens/
       🔹 adminScreens/
       🔹 components/
    📄 MainActivity.kt        # NavHost, routing, and deep link handling
    📄 RideNavigationStarter.kt  # Launches Google Maps navigation


**▶️ Getting Started**

  🔧 Prerequisites
  
      Android Studio Hedgehog or later
  
      Firebase project with:
  
          Firestore, Auth, Messaging, Functions, and Storage enabled
  
      Google Cloud Project with:
  
          Maps SDK for Android
  
          Places API
  
          Directions API
  
          Static Maps API (if map images are used)
  
  ⚙️ Setup
  
    Clone the repo:
  
        git clone https://github.com/aviel3899/WePool.git
  
    Open in Android Studio and sync Gradle.
  
    Configure Firebase:
  
        Place your google-services.json file in the /app directory.
  
        Ensure your Firebase project includes the necessary APIs and services.
  
    Run the app on an emulator or physical Android device (with Google Play Services).

   
**📸 Screenshots**

  | Home Page | Preferred Locations | Role Selection |
|-----------|---------------------|----------------|
| <a href="https://github.com/user-attachments/assets/e0ae707e-1235-4a14-8aa1-8c223c17f430"><img src="https://github.com/user-attachments/assets/e0ae707e-1235-4a14-8aa1-8c223c17f430" width="250"/></a> | <a href="https://github.com/user-attachments/assets/e22fc62f-0933-4414-b923-356782fb2cbf"><img src="https://github.com/user-attachments/assets/e22fc62f-0933-4414-b923-356782fb2cbf" width="250"/></a> | <a href="https://github.com/user-attachments/assets/204f5a1c-ae4d-4f33-bd1f-cfc9b372896b"><img src="https://github.com/user-attachments/assets/204f5a1c-ae4d-4f33-bd1f-cfc9b372896b" width="250"/></a> |

| Admin Menu | Admin Ride View | Ride Creation |
|------------|------------------|----------------|
| <a href="https://github.com/user-attachments/assets/e31fb35d-e091-4f8c-b76a-e77b87709b49"><img src="https://github.com/user-attachments/assets/e31fb35d-e091-4f8c-b76a-e77b87709b49" width="250"/></a> | <a href="https://github.com/user-attachments/assets/27c948fc-fdfe-47ca-83b7-1b14467d5fc9"><img src="https://github.com/user-attachments/assets/27c948fc-fdfe-47ca-83b7-1b14467d5fc9" width="250"/></a> | <a href="https://github.com/user-attachments/assets/58610b1e-7450-4bee-a79d-7b355afcd7d6"><img src="https://github.com/user-attachments/assets/58610b1e-7450-4bee-a79d-7b355afcd7d6" width="250"/></a> |

| Join a Ride | Active Rides | Route Map |
|-------------|--------------|------------|
| <a href="https://github.com/user-attachments/assets/8d8a9b44-e738-49e1-9e15-d0b6bbebd9cb"><img src="https://github.com/user-attachments/assets/8d8a9b44-e738-49e1-9e15-d0b6bbebd9cb" width="250"/></a> | <a href="https://github.com/user-attachments/assets/a042baad-5785-49e1-90d7-510a587ce8fb"><img src="https://github.com/user-attachments/assets/a042baad-5785-49e1-90d7-510a587ce8fb" width="250"/></a> | <a href="https://github.com/user-attachments/assets/8bfa36b8-5c46-4106-9f87-c392d8e7e5ec"><img src="https://github.com/user-attachments/assets/8bfa36b8-5c46-4106-9f87-c392d8e7e5ec" width="250"/></a> |
  


👥 Contributors

👤 Aviel Smolanski

    Linkedln - https://www.linkedin.com/in/aviel-smolanski-229099247/

    Email - aviel3899@gmail.com

👤 Elior Uzan

    Linkedln - https://www.linkedin.com/in/elior-uzan-b36548228/

    Email - elioruzan06@gmail.com


**📄 License**

This repository is licensed for academic purposes only.All content is part of a final project submission at Afeka College.
