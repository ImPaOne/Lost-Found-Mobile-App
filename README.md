"# Lost-Found-Mobile-App"

# FindIt LK - Lost & Found Mobile Application

A comprehensive Android application designed to connect individuals who have lost items with those who have found them. This app serves as a bridge between the finder and the loser, leveraging community effort to reunite people with their belongings.

## Features

- **Authentication**: Secure login and registration with email verification.
- **User Profiles**: Management of user personal information.
- **Item Management**:
  - **Post Lost Items**: Users can create posts describing lost items with details and location.
  - **Post Found Items**: Users can create posts describing items they have found.
  - **Image Upload**: Upload images for lost/found items.
- **Messaging System**: Built-in chat functionality to connect finders and losers.
- **Home Feed**: Displays recent lost and found items with category filtering.
- **Search Functionality**: Search for items by keywords or categories.
- **Notifications**: Real-time push notifications for new messages and post updates.
- **Analytics**: Visual dashboards to track application metrics and performance.

## Tech Stack

- **Language**: Kotlin
- **UI Components**:
  - Jetpack Compose (For modern UI elements and transitions)
  - Material Design 3
  - Lottie Animations
  - WebView (For embedded Leaflet maps)
- **Architecture**:
  - MVVM (Model-View-ViewModel)
- **Libraries**:
  - **Firebase**: Authentication, Firestore Database, Storage, Cloud Messaging
  - **Coil**: Image loading and caching
  - **Material Icons**: Vector icons

## Getting Started

### Prerequisites

- Android Studio installed
- Android device or emulator
- A Firebase project with:
  - Email/Password Authentication enabled
  - Cloud Firestore database set up
  - Firebase Storage configured
  - Firebase Cloud Messaging configured

### Setup

1. Clone the repository.
2. Open the project in Android Studio.
3. Add your `google-services.json` file to the `app/` directory.
4. Ensure all Firebase configurations are correctly set up in the Firebase Console and linked to your Android app.
5. Run the app on an emulator or physical device.

## Project Structure

The project follows a standard Android application structure with MVVM architecture:

```
com.group3.lostandfound/
├── data/
│   ├── model/          # Data classes (User, Item, Message, etc.)
│   ├── repository/     # Data repositories for Firebase operations
│   └── service/        # Service classes (e.g., FCMService)
├── ui/
│   ├── auth/           # Login, Register, Forgot Password screens
│   ├── home/           # Home screen components (PostCard, FeedViewModel)
│   ├── item/           # Item creation and details screens
│   ├── messaging/      # Chat and message handling
│   ├── main/           # Main activity and navigation
│   ├── analytics/      # Analytics and dashboard components
│   └── common/         # Reusable UI components and helpers
└── utils/              # Utility functions and helpers
```

## Usage

1. **Login/Register**: Users can sign up with their email and password or log in to their existing account.
2. **Create Posts**: Navigate to the Home screen and tap the '+' button to create a new Lost or Found item post.
3. **Browse Feed**: Scroll through the Home Feed to view recent posts from the community.
4. **Chat**: When you find an item that matches a lost post, or vice versa, use the built-in chat to communicate and arrange the return.
5. **View Profile**: Access your profile to edit your details or view your posts.
