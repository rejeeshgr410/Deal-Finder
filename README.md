# Deal Finder 🏷️

**Deal Finder** is an end-to-end platform for discovering and publishing grocery and retail deals, promotions, and flyers across the UAE.

The project is architected into two core applications sharing a unified Firebase backend:

```
Deal Finder/
├── user-app/       # Consumer App (Native Android / Kotlin)
└── merchant-app/   # Merchant & Admin Portal (React / Vite / Capacitor)
```

---

## 📱 Applications

### 1. User App (`user-app/`)
* **Role:** Consumer-facing mobile app (Frontend)
* **Tech Stack:** Android (Kotlin), Jetpack Compose, Material Design 3, Coroutines/Flow
* **Features:**
  * Browse discounts, store flyers, and flash sales by category and location
  * Real-time search with OCR and Gemini AI deal extraction
  * Bookmark favorite stores and deals
  * Interactive map and store locator
* **Prerequisites:** Android Studio (Ladybug or newer), JDK 17/21, Android SDK (API 34+)

### 2. Merchant App (`merchant-app/`)
* **Role:** Business and admin management portal (Backend / Merchant Portal)
* **Tech Stack:** React 19, TypeScript, Vite, Tailwind CSS v4, Lucide Icons, Capacitor
* **Features:**
  * Deal and promotion flyer upload & management
  * Automated flyer processing using Gemini AI and Tesseract OCR
  * Store branch and profile configuration
  * Analytics on impressions and user engagements
* **Prerequisites:** Node.js (v18+), npm

---

## ☁️ Shared Backend Infrastructure

Both applications share a single Firebase project for real-time synchronization:
* **Firebase Authentication:** Secure customer and merchant auth
* **Cloud Firestore:** Real-time database for deals, stores, categories, and analytics
* **Firebase Storage:** High-resolution flyer scans, store logos, and promotional imagery
* **Google Gemini AI:** Intelligent extraction and parsing of deal flyers

---

## 🚀 Quick Start

### Running the User App
```bash
cd user-app
# Open this folder in Android Studio
# Set up your google-services.json and build:
./gradlew assembleDebug
```

### Running the Merchant App
```bash
cd merchant-app
npm install
npm run dev
# Portal will start at http://localhost:3000
```

---

## 🌿 Git Branch Structure

* **`main`**: Full monorepo containing both the User App and Merchant App
* **`user-app`**: Dedicated branch focusing on the consumer Android application
* **`merchant-app`**: Dedicated branch focusing on the merchant and admin portal

---

## 📄 License
Private & Confidential - All Rights Reserved.
