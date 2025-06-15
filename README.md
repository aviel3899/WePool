**WePool 🚗**

WePool is a smart internal carpool management application designed for organizations, developed as a graduation project at Afeka College of Engineering.

The app helps employees of the same company coordinate safe, efficient, and eco-friendly shared rides. With an intelligent ride-matching engine and deep integration with Google Maps and Firebase, WePool improves commuting experiences while promoting sustainability and trust within the workplace.


**🎬 Watch the Demo**

📺 YouTube Video: [https://youtu.be/InsN7Mb17d0](https://www.youtube.com/watch?v=InsN7Mb17d0)


**🚀 Features**

  🔐 Role-based access system: Driver, Passenger, HR Manager, Admin.

  🧠 Intelligent ride-matching:

      Based on arrival time, route detour tolerance, and available seats

      Smart offline matching algorithm to avoid unnecessary expensive API calls

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
       🔹 HrManagerScreens/
       🔹 RideHistoryScreens/
       🔹 MainScreens/
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

 | Logo | Login | Signup |
|-----------|---------------------|----------------|
| ![logo](https://github.com/user-attachments/assets/11ed3168-f985-4800-b1b1-64354fe48f00)|![login](https://github.com/user-attachments/assets/cfab6b96-d705-4626-9569-cf109ff65228)|![signup](https://github.com/user-attachments/assets/d6842e9e-5888-4a80-a5bf-45a3bb834319)|

  | Home Page | Terms & Condotions | Role Selection |
|-----------|---------------------|----------------|
| ![intermediateScreen](https://github.com/user-attachments/assets/a699aaec-e18b-457e-982d-66f18231dcbd)|![terms and conditions](https://github.com/user-attachments/assets/3fae24c2-992d-47bc-b5d7-e6e9ff616ce1)|![role selection](https://github.com/user-attachments/assets/4bfa47fb-2da2-405a-a179-43c1df9ddd28)|

| Driver Menu | Choose Ride Direction | Ride Creation |
|------------|------------------|----------------|
|![driver menu](https://github.com/user-attachments/assets/b13bf1ad-73c8-490a-bf4e-73776b13ae0d)|![choose direction](https://github.com/user-attachments/assets/a1ef6acb-17a8-4e03-b628-72db0fe0eaae)|![ride creation](https://github.com/user-attachments/assets/4120191d-9603-4dba-b117-dd37056586f2)|

| Driver Active Rides | Passenger Menu | Join a Ride |
|------------|------------------|----------------|
|![driver active rides](https://github.com/user-attachments/assets/a2b28d92-e553-4d9c-b71b-bb4ef14a553b)|![passenger menu](https://github.com/user-attachments/assets/2d38e966-9599-4f69-9289-5aa952af027b)|![join a ride](https://github.com/user-attachments/assets/ac21e2fc-72ec-45ca-b8c8-613dca12d198)|

| Passenger Active Rides | Admin Menu | Admin Compamies Manage |
|-------------|--------------|------------|
|![passenger active rides](https://github.com/user-attachments/assets/9c643da7-4fbd-4fae-9598-c2bfe8983b10)|![admin menu](https://github.com/user-attachments/assets/d45b3674-443f-464a-9476-467ea284964f)|![copmanies screen](https://github.com/user-attachments/assets/cc853c14-16e6-4b65-ad2b-55629c39c4db)|

|Company Filter | Add Company | Company Employees |
|-------------|--------------|------------|
|![company filter](https://github.com/user-attachments/assets/96e84f40-7bad-47df-aaf6-22564c431ea3)|![add company](https://github.com/user-attachments/assets/ac321157-d5ca-4a1a-9826-807fc1155b36)|![company employees](https://github.com/user-attachments/assets/f3c16063-ffee-42a1-9f8d-ac2414f1a4e3)|

| Company's HR Manager | Admin Users Manager | User Filter |
|-------------|--------------|------------|
|![hr manager selection](https://github.com/user-attachments/assets/32e5e25b-11fb-4ee3-8880-d52e2606b80a)|![users screen](https://github.com/user-attachments/assets/146d9eb8-58c2-4ce1-a51c-1ab0ea53f777)|![user filter](https://github.com/user-attachments/assets/09d4a42a-385f-4574-8e92-2fb2917a1395)|

| User Details | Deactivate User | User Deactivated |
|-------------|--------------|------------|
|![user details](https://github.com/user-attachments/assets/00842f81-4abc-4830-9764-1992b6381cf4)|![deactivate user](https://github.com/user-attachments/assets/b0a70e24-98ed-4a21-b72b-1049b19cf062)|![user deactivated](https://github.com/user-attachments/assets/49dc8a36-2582-4b65-86e1-e9524707482c)

| User Deactivated Home Page | Admin Rides Manage | Ride Filter |
|-------------|--------------|------------|
|![user deactivated tryimg to use the app](https://github.com/user-attachments/assets/88b19999-ed74-4a24-be57-cd414a61044e)|![rides screen](https://github.com/user-attachments/assets/3ac97146-961a-47b6-a1e5-3e8a4df10d75)|![ride filter](https://github.com/user-attachments/assets/d94c90dc-5a79-4bca-a729-0468d8fd9de4)|

| Ride Details | Ride Dynamic Map | Ride Passengers List |
|-------------|--------------|------------|
|![ride deatils](https://github.com/user-attachments/assets/005d5f9b-46eb-47b3-adf0-a95cc4ec6195)|![ride map](https://github.com/user-attachments/assets/aa78b71a-a8c9-4dd4-846b-0aad7daff39f)|![passenger list](https://github.com/user-attachments/assets/8b996564-0111-44c5-856e-3aee3d08b0a7)|

| Paseengers Details | HR Manager Menu | HR Manager Company Manage |
|-------------|--------------|------------|
|![passenger details](https://github.com/user-attachments/assets/e27387ad-11fc-4958-b861-e0f28c05b797)|![hr manager menu](https://github.com/user-attachments/assets/cba8d5bc-869b-4b95-8ca9-ed167e28c761)|![hr manager company settings](https://github.com/user-attachments/assets/f384779c-81e2-4b59-a510-2ae24439e3fd)|

| Update Details | Ride History Filter | Preferred Locations |
|-------------|--------------|------------|
|![update details](https://github.com/user-attachments/assets/f87448cd-bbd9-4e90-b994-40d804d492f4)|![ride history search](https://github.com/user-attachments/assets/a643a818-66ed-4f1a-90f1-c205a6f59d44)|![preferred locations](https://github.com/user-attachments/assets/28ea2c76-2fb5-4af4-bf65-a92560cf59a5)|


👥 Contributors

👤 Aviel Smolanski

    Linkedln - https://www.linkedin.com/in/aviel-smolanski-229099247/

    Email - aviel3899@gmail.com

👤 Elior Uzan

    Linkedln - https://www.linkedin.com/in/elior-uzan-b36548228/

    Email - elioruzan06@gmail.com


**📄 License**

This repository is licensed for academic purposes only.All content is part of a final project submission at Afeka College.
