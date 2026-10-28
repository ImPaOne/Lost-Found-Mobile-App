# Foundly - Lost & Found Mobile Application

> **Tagline:** _Lost less. Found faster._

Foundly is a modern, community-driven Android application engineered to bridge the gap between people who have lost their belongings and good Samaritans who have found them across Sri Lanka. Built using 100% Kotlin and modern Android architecture, Foundly provides real-time reporting, interactive geo-spatial mapping, peer-to-peer messaging, automated printable flyers with scannable QR codes, a community trust engine, and a complete administrative moderation suite.

---

## Key Features

### 1. Item Reporting & Management

- **Report Lost & Found Items:** Multi-step submission flow capturing item title, category, detailed descriptions, date/time, and reward offerings.
- **Location Pinning:** Precise location capture with an interactive Leaflet map picker mode.
- **Media Uploads:** Direct image capture and gallery uploads processed via Cloudinary CDN or Firebase Storage.
- **Status Lifecycle:** Comprehensive item state tracking (`LOST`, `FOUND`, `RESOLVED`, `CLAIMED`).
- **Owner Resolution:** Reporters can mark their items as resolved and reunited, rewarding community members with trust points and badges.

### 2. Interactive Geo-Spatial Map

- **Leaflet.js & OpenStreetMap Integration:** High-performance interactive map embedded inside an Android `WebView` with custom JavaScript interface bridging map events directly to native Kotlin components.
- **Color-Coded Status Pins:** Visual distinction between Lost items (Red) and Found items (Blue).
- **Interactive Card Previews:** Tapping any pin displays a detailed bottom preview card with instant navigation to the item's full details.
- **Location Filtering:** Filter pins by status (`All`, `Lost`, `Found`, `Today`) with one-tap GPS centering.

### 3. Real-Time Peer-to-Peer Communication

- **Live In-App Chat:** Real-time messaging powered by Firebase Realtime Database (`/chats` and `/user_conversations`).
- **Rich Messaging:** Message replies, image sharing, read receipts, and message deletion ("Delete for me" and "Delete for everyone").
- **WhatsApp Direct Connect:** Deep integration launching WhatsApp (`wa.me`) directly with pre-formatted item inquiry messages.
- **Floating Chat Head Overlay:** Draggable system alert window overlay (`FloatingOverlayService`) allowing users to multitask across apps while keeping active lost-and-found chats accessible.

### 4. Dynamic Community Flyers & QR Codes

- **Automated Poster Generation:** Built-in `FlyerGenerator` renders high-resolution emergency Lost/Found flyers on an Android Canvas.
- **Scannable QR Codes:** Uses the ZXing barcode library to embed QR codes linking directly to the item's web and deep link (`https://foundly.lk/item/{id}`).
- **Native Android ShareSheet:** Instant sharing of generated flyers to WhatsApp, Facebook, Instagram, email, or device storage.

### 5. Community Trust & Gamification Engine

- **Trust Scores:** Algorithmic trust ratings (e.g., "95% Verified Community Member") based on successful returns, account verification, and community feedback.
- **Verification Badges:** Multi-level verification for Email, Phone number, and Government ID.
- **Achievement System:** Unlockable milestone badges including _First Find_, _Community Helper_, _Trusted Reporter_, and _Reunited Hero_.

### 6. Administration, Moderation & Analytics

- **Admin Dashboard:** Centralized operations console tracking active cases, resolution rates, user metrics, and system health.
- **Content Moderation Queue:** Review, approve, edit, flag, or remove suspicious/inappropriate listings.
- **User Access Management:** Audit user trust scores, grant verified badges, or ban abusive users.
- **Real-Time Analytics:** Live-streamed metric aggregations from Firebase:
  - Custom Canvas-rendered animated `PieChartView` for item category distribution.
  - City-level resolution benchmarks (Colombo, Kandy, Galle).
  - Density-gradient Leaflet.js geographic heat map.
- **Global Announcements:** Broadcast urgent platform alerts and safety advisories to all users.
- **System Maintenance:** Complete database JSON backup and restore capabilities directly from the admin panel.

### 7. Background Services & Notifications

- **Foreground Synchronization:** `AppNotificationService` maintains real-time listeners for live messages, match alerts, and community announcements even when the app is in the background.
- **Firebase Cloud Messaging (FCM):** Push notifications with custom Apple-style sound effects (`apple_tri_tone`, `apple_aurora`, `apple_chord`).

### 8. Localization & Personalization

- **Trilingual Localization:** Seamless runtime switching between English (`en`), Sinhala (`si`), and Tamil (`ta`) via `LocaleHelper`.
- **Material Design 3 Theme:** Full support for dynamic Day and Night (Dark Mode) themes.
- **Saved Items / Bookmarks:** Save important listings for fast access from the profile drawer.

---

## Architecture & Technology Stack

Foundly follows the recommended Android **MVVM (Model-View-ViewModel)** architectural pattern combined with the **Repository Pattern** and Clean Architecture principles to ensure maintainability, scalability, and testability.

### Technology Stack Matrix

| Layer                     | Technologies & Libraries                                                                                                                           |
| ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Language**              | 100% Kotlin                                                                                                                                        |
| **Architecture**          | MVVM (Model-View-ViewModel), Repository Pattern, Clean Separation                                                                                  |
| **UI & Layouts**          | Native Android XML Layouts, View Binding, Material Design 3 Components (`MaterialCardView`, `BottomSheetDialog`, `Snackbar`, `Chip`), `ViewPager2` |
| **Custom Views**          | Custom Canvas Drawing (`PieChartView`)                                                                                                             |
| **Database**              | Firebase Realtime Database (real-time JSON tree synchronization)                                                                                   |
| **Authentication**        | Firebase Authentication (Email/Password authentication & role management)                                                                          |
| **Cloud Storage & CDN**   | Cloudinary CDN API (direct image upload via multipart/form-data) & Firebase Storage                                                                |
| **Push Notifications**    | Firebase Cloud Messaging (FCM) & Android Foreground Services                                                                                       |
| **Geo-Spatial / Maps**    | Leaflet.js & OpenStreetMap embedded via Android `WebView` with bidirectional `JavascriptInterface`                                                 |
| **QR Code Generation**    | ZXing Core (`com.google.zxing:core:3.5.3`)                                                                                                         |
| **Concurrency & Async**   | Kotlin Coroutines (`kotlinx-coroutines-android`, `kotlinx-coroutines-play-services`), Kotlin `StateFlow`                                           |
| **Image Loading & Cache** | Custom zero-dependency asynchronous `ImageLoader` with in-memory `LruCache` supporting URLs, Base64 data URIs, Content URIs, and local files       |
| **Deep Linking**          | Android Intent Filters for `foundly://item/{id}` and `https://foundly.lk/item/{id}`                                                                |

---

## Project Structure

```
Lost-Found-Mobile-App/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/group3/lostandfound/
│   │   │   │   ├── adapter/                    # RecyclerView & ViewPager adapters
│   │   │   │   │   ├── ChatAdapter.kt          # Chat message binding (sent/received/reply/images)
│   │   │   │   │   └── ExploreAdapter.kt       # Grid and list report item cards binding
│   │   │   │   ├── model/                      # Data classes and UI state definitions
│   │   │   │   │   ├── ItemReport.kt           # Lost/Found item schema and Firebase parsers
│   │   │   │   │   └── UiStates.kt             # Sealed classes & StateFlow UI states
│   │   │   │   ├── repository/                 # Data access layer interfacing with Firebase
│   │   │   │   │   ├── AuthRepository.kt       # User authentication and registration
│   │   │   │   │   ├── ChatRepository.kt       # Conversation metadata & chat messages
│   │   │   │   │   ├── ExploreRepository.kt    # Search queries and item filtering
│   │   │   │   │   ├── ItemRepository.kt       # CRUD operations for /items
│   │   │   │   │   ├── MessagesRepository.kt   # Inbox threads and conversation lists
│   │   │   │   │   └── ReportRepository.kt     # Item creation and status transitions
│   │   │   │   ├── service/                    # Background & foreground Android services
│   │   │   │   │   ├── AppNotificationService.kt # Foreground sync for live alerts & messages
│   │   │   │   │   ├── FCMService.kt           # Firebase Cloud Messaging handler
│   │   │   │   │   └── FloatingOverlayService.kt # Draggable floating chat head overlay
│   │   │   │   ├── util/                       # Reusable utility classes and helpers
│   │   │   │   │   ├── CloudinaryManager.kt    # Direct image upload to Cloudinary CDN
│   │   │   │   │   ├── FlyerGenerator.kt       # Canvas poster rendering & ZXing QR generation
│   │   │   │   │   ├── ImageLoader.kt          # Zero-dependency LruCache async image loader
│   │   │   │   │   ├── LocaleHelper.kt         # Runtime language switching (en, si, ta)
│   │   │   │   │   └── ModernToast.kt          # Custom styled snackbars and alert feedback
│   │   │   │   ├── viewmodel/                  # MVVM ViewModels managing UI logic & state
│   │   │   │   │   ├── AuthViewModel.kt
│   │   │   │   │   ├── CreateReportViewModel.kt
│   │   │   │   │   ├── DetailsViewModel.kt
│   │   │   │   │   ├── ExploreViewModel.kt
│   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   ├── MessagesViewModel.kt
│   │   │   │   │   └── ReportsViewModel.kt
│   │   │   │   ├── AdminFragment.kt            # Admin operations overview & quick tools
│   │   │   │   ├── AnalyticsFragment.kt        # Analytics dashboard, pie chart & heat map
│   │   │   │   ├── BottomNavFragment.kt        # Reusable bottom navigation controller
│   │   │   │   ├── ChatFragment.kt             # Peer-to-peer real-time chat screen
│   │   │   │   ├── ChatViewModel.kt            # Chat screen ViewModel
│   │   │   │   ├── CreateReportFragment.kt     # Multi-step report submission flow
│   │   │   │   ├── DetailsFragment.kt          # Full item details, actions & WhatsApp link
│   │   │   │   ├── ExploreFragment.kt          # Search, category filters & view mode toggle
│   │   │   │   ├── FirebaseManager.kt          # Centralized Firebase database instance & observers
│   │   │   │   ├── HomeFragment.kt             # Main dashboard with urgent carousel & stats
│   │   │   │   ├── LoginFragment.kt            # Authentication login screen
│   │   │   │   ├── MainActivity.kt             # Main container activity & deep link router
│   │   │   │   ├── MapFragment.kt              # Leaflet.js interactive map & location picker
│   │   │   │   ├── MessagesFragment.kt         # Inbox conversations list
│   │   │   │   ├── Models.kt                   # Core data models (User, Chat, Analytics, etc.)
│   │   │   │   ├── ModerationFragment.kt       # Admin moderation queue (approve/flag/delete)
│   │   │   │   ├── NavHelper.kt                # Navigation extension functions
│   │   │   │   ├── NavigationHelper.kt         # Fragment transition routing
│   │   │   │   ├── NotificationsFragment.kt    # In-app notifications history list
│   │   │   │   ├── OnboardingFragment.kt       # Onboarding step 1: Lost items
│   │   │   │   ├── OnboardingFoundFragment.kt  # Onboarding step 2: Found items
│   │   │   │   ├── OnboardingReuniteFragment.kt# Onboarding step 3: Reunite community
│   │   │   │   ├── PieChartView.kt             # Custom Canvas donut/pie chart view
│   │   │   │   ├── ProfileFragment.kt          # User profile, statistics & action links
│   │   │   │   ├── ReportsFragment.kt          # "My Reports" tracking & resolution manager
│   │   │   │   ├── SavedItemsFragment.kt       # User bookmarked/saved items
│   │   │   │   ├── SettingsFragment.kt         # Language, theme, notification preferences
│   │   │   │   ├── ShareFlyerFragment.kt       # Standalone flyer preview & share sheet
│   │   │   │   ├── SignUpFragment.kt           # Account registration screen
│   │   │   │   ├── SplashActivity.kt           # Brand splash screen with slogan
│   │   │   │   ├── TrustFragment.kt            # Trust scores, badges & achievements
│   │   │   │   ├── UiKitFragment.kt            # Component library showcase
│   │   │   │   └── UserManagementFragment.kt   # Admin user management & banning
│   │   │   ├── res/
│   │   │   │   ├── layout/                     # XML layout resources
│   │   │   │   ├── raw/                        # Apple-style notification audio chimes
│   │   │   │   ├── values/                     # Colors, styles, English strings
│   │   │   │   ├── values-si/                  # Sinhala localized strings
│   │   │   │   ├── values-ta/                  # Tamil localized strings
│   │   │   │   └── values-night/               # Dark theme color overrides
│   │   │   └── AndroidManifest.xml             # Permissions, activities, and services
│   │   └── build.gradle.kts                    # App-level build script & dependencies
│   └── google-services.json                    # Firebase service configuration
├── gradle/
│   └── libs.versions.toml                      # Version catalog for dependencies
├── build.gradle.kts                            # Root build script
├── settings.gradle.kts                         # Project repository settings
└── README.md
```

---
