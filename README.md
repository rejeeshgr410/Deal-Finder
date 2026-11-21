# Deal Finder UAE 🏷️

**Deal Finder** is an all-in-one **UAE offers, price comparison, and smart-shopping aggregator** connecting shoppers with live supermarket deals, near-expiry clearance bundles, digital coupons, loyalty rewards, and weekly store flyers.

Rather than being just a digital flyer viewer, Deal Finder brings together multi-store price intelligence, basket optimization, anti-food-waste clearance combos, and checkout rewards in a single consumer app backed by a real-time merchant portal.

```
Deal Finder/
├── user-app/       # Consumer App (Native Android / Kotlin / Jetpack Compose)
├── merchant-app/   # Merchant & Admin Portal (React / TypeScript / Vite / Capacitor)
└── firebase/       # Cloud Functions, Firestore & Storage rules, AI extraction
```

---

## 📱 Applications

### 1. Consumer App (`user-app/`)
* **Role:** Consumer-facing smart shopping & deals aggregator (Android)
* **Tech Stack:** Kotlin, Jetpack Compose, Material Design 3, Room, Coil, Coroutines/Flow, Firebase
* **Key Features:**
  * **Clearance & Near-Expiry Bundles:** Dedicated **Clearance** hub where supermarkets sell near-expiry product combos (e.g., Nestlé & pantry bundles) at **up to 90% off** total retail price—displaying itemized retail values, slashed original totals, combo offer prices, discount percentages, and expiry dates.
  * **Multi-Store Price Comparison:** Compare identical products and unit prices across nearby UAE supermarkets (Carrefour, LuLu Hypermarket, Nesto, Viva, and more) with Emirate/GPS distance sorting and advanced filters.
  * **Smart Lists & Basket Optimizer:** Build shopping lists, track monthly budgets, and automatically calculate the **cheapest single store** or **split-store strategy** to maximize savings.
  * **Trending Products & Top Deals Today:** Auto-scrolling brand showcase carousel and curated top deals ranked by savings and relevance.
  * **Loyalty Card Wallet:** Quick-access **Wallet** from the top-right header to store and scan retail loyalty barcodes (LuLu Happiness, Carrefour SHARE, Nesto INAAM, etc.) right at checkout.
  * **Exclusive Coupons:** Search, filter, save, and one-tap copy promo codes for supermarkets, electronics, and lifestyle partners.
  * **Nearby Flyers & Store Locator:** Location-aware store circulars, branch addresses, and directions across all 7 UAE Emirates.
  * **Bilingual & Theme-Ready:** Full English and Arabic (`RTL`) localization, Light/Dark modes, offline caching, and configurable price-drop/expiry notifications.
* **Prerequisites:** Android Studio (Ladybug or newer), JDK 17/21, Android SDK (API 34+)

### 2. Merchant & Admin Portal (`merchant-app/`)
* **Role:** Retailer onboarding, deal publishing, and catalog management portal
* **Tech Stack:** React 19, TypeScript, Vite, Tailwind CSS v4, Lucide Icons, Capacitor
* **Features:**
  * Deal, flyer, and promotion publishing & lifecycle management
  * Automated flyer & product extraction using Google Gemini AI / Vertex AI and Tesseract OCR
  * Branch geolocation, store profile configuration, and engagement analytics
* **Prerequisites:** Node.js (v18+), npm

---

## ☁️ Shared Backend Infrastructure

Both applications share a unified Firebase & Google Cloud backend:
* **Firebase Authentication:** Customer and merchant sign-in (Email/Password, Google Sign-In, Guest mode)
* **Cloud Firestore & Room Cache:** Real-time catalog synchronization with offline-first local caching
* **Cloud Storage:** Flyer assets, store logos, and product imagery
* **Vertex AI / Gemini AI:** Intelligent flyer parsing, SKU extraction, and deal structuring

---

## 🚀 Quick Start

### Running the Consumer App
```bash
cd user-app
# Open this folder in Android Studio, configure google-services.json, then build:
./gradlew assembleDebug
```

### Running the Merchant Portal
```bash
cd merchant-app
npm install
npm run dev
# Portal starts at http://localhost:3000
```

---

## 🌿 Git Branch Structure

* **`main`**: Full monorepo containing the Consumer Android App, Merchant Portal, and Firebase backend
* **`user-app`**: Dedicated branch for the consumer Android application
* **`merchant-app`**: Dedicated branch for the merchant and admin portal

---

## 📄 License
Private & Confidential - All Rights Reserved.
