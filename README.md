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
