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
