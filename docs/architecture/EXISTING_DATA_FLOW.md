# Existing Data Flow & Repository Architecture

**Baseline Commit:** `4ee27a1` (`main` + 22 uncommitted WIP files on `phase/s0-baseline`)  
**Inspected:** 18 November 2026  

---

## 1. System Topology (Observed)

```text
+-----------------------------------+        +-----------------------------------+
|      Merchant / Admin Web App     |        |      Consumer Android App         |
|  merchant-app/ (React 19 + Vite   |        |  user-app/ (Kotlin + Compose      |
|  + TypeScript + Capacitor 8.6)    |        |  + Room cache + Firebase SDK)     |
+-----------------+-----------------+        +-----------------+-----------------+
                  |                                            |
                  | Firebase JS SDK (firebase.ts)              | Firebase Android SDK (FirebaseManager.kt)
                  |                                            |
                  v                                            v
        +---------------------------------------------------------------+
        | Firebase Project: gen-lang-client-0615495350 (102162589559)   |
        |                                                               |
        |  - Firebase Auth (Email/Password + Google Sign-In)            |
        |  - Named Firestore DB:                                        |
        |    ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096             |
        |    (CRITICAL: NOT the "(default)" database)                   |
        |  - Storage Bucket:                                            |
        |    gen-lang-client-0615495350.firebasestorage.app             |
        +---------------------------------------------------------------+
                  ^
                  | Direct browser Gemini API call (flyerExtractor.ts)
                  | using VITE_GEMINI_API_KEY / localStorage key
                  v
        +-----------------------------------+
        | Google Gemini API (gemini-2.5-flash)
        +-----------------------------------+
```

> **Note:** No Firebase Cloud Functions directory or trusted server-side API currently exists in the repository. All Firestore writes, Storage uploads, Gemini API calls, and notification log writes are executed directly from client code.

---

## 2. Firebase Configuration & Rules Inventory

| Component | Location | Status | Notes |
|---|---|---|---|
| Web Firebase config | `merchant-app/firebase-applet-config.json`, `merchant-app/src/firebase.ts` | Observed | Targets `gen-lang-client-0615495350` and named DB `ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096` |
| Web Hosting & rules config | `merchant-app/firebase.json`, `merchant-app/.firebaserc` | Observed | Hosting `dist/`, Firestore named DB rules, Storage rules |
| Android Firebase config | `user-app/app/google-services.json`, `user-app/app/src/main/java/com/example/data/FirebaseManager.kt` | Observed | Initializes default `FirebaseApp` and calls `FirebaseFirestore.getInstance(FIRESTORE_DATABASE_ID)` |
| Canonical Firestore rules (WIP) | `merchant-app/firestore.rules`, `user-app/firestore.rules` | Identical SHA256 (`1D1AD667...`) | Includes uncommitted coupon-scoping and user subcollection rules; live deployed ruleset on Firebase console NOT independently verified (`firebase` CLI not installed) |
| Stale Firestore rules backup | `user-app/firestore.rules.STALE-DO-NOT-DEPLOY.bak` | Stale backup | Should not be deployed |
| Storage rules (merchant) | `merchant-app/storage.rules` | Active local rules | Gates writes on `isSignedIn()` + `<25MB` + image/PDF due to `firestore.get()` named-DB limitation |
| Storage rules (user-app) | `user-app/storage.rules` | Divergent / stale | References `firestore.get(/databases/(default)/...)` and `/ocr_uploads/{imageId}` with `<10MB` limit and legacy role names (`superadmin`, `retailer`) |

---

## 3. Collection Read/Write Access Matrix (Observed in Code & `merchant-app/firestore.rules`)

| Collection / Path | Merchant Portal (`merchant-app`) | Consumer App (`user-app`) | Current Rules (`merchant-app/firestore.rules`) |
|---|---|---|---|
| `users/{uid}` | Reads/writes own doc on auth (`AuthContext.tsx`); admin reads/updates via `Users.tsx` | Reads/writes own doc (`AuthRepository.kt`, `FirestoreDealFinderDao.kt`) | `read`: self, moderator, or same-retailer owner; `create`: `request.auth.uid == userId` (unrestricted fields); `update`: admin or self without `role`/`retailerId` change; `delete`: admin |
| `users/{uid}/shoppingLists/{listId}/items` | Not used | Synced with Room DB (`ShoppingListRepository.kt` currently listens to `"My Lists"`) | `read, write`: signed-in owner (added in uncommitted rules diff) |
| `users/{uid}/savedDeals`, `savedFlyers`, `savedCoupons`, `followedRetailers`, `searchHistory`, `alerts` | Not used | Read/write per user (`FirestoreDealFinderDao.kt`) | `read, write`: signed-in owner (added in uncommitted rules diff) |
| `retailers/{id}` | Full CRUD via `useCollection('retailers')` (`Retailers.tsx`, `RetailerAccount.tsx`) | Read `where('status', '==', 'published').limit(100)` (`FirestoreDealFinderDao.kt`) | `read`: public (`true`); `create, delete`: admin; `update`: moderator or `isRetailerStaff(retailerId)` |
| `branches/{id}` | Full CRUD (`RetailerBranches.tsx`); seeded with random coords in `seed_d4d_data.ts` | Read all (`db.collection("branches").asFlow()`) | `read`: public (`true`); `create/update/delete`: moderator/admin or `isRetailerStaff` |
| `flyers/{id}` | Created in `OcrCenter.tsx` (`ocr_review`), bulk-published in `OcrReview.tsx` | Read `where('status', '==', 'published').limit(100)`, cached in Room (`CachedFlyerDao`) | `read`: public (`true`); `create/update`: moderator, `data_entry`, or `isRetailerStaff`; `delete`: admin or `isRetailerStaff` |
| `products/{id}` | Created individually in `Products.tsx` or via `Promise.all(addProduct(...))` in `OcrReview.tsx` | Read `where('status', '==', 'published').limit(100)`; search uses `array-contains` on `searchKeywords` (`limit(100)`) | `read`: public (`true`); `create/update`: moderator, `data_entry`, or `isRetailerStaff`; `delete`: admin or `isRetailerStaff` |
| `promotions/{id}` | CRUD via `Promotions.tsx` | Read `where('status', '==', 'published').limit(100)` | `read`: public (`true`); `create`: moderator, `data_entry`, or `isRetailerStaff`; `update/delete`: moderator/admin or `isRetailerStaff` |
| `coupons/{id}` | Managed in merchant app | Read `where('status', '==', 'published').limit(100)` (`CouponsScreen.kt`) | `read`: public (`true`); global coupons admin-only, shop coupons admin or `isRetailerStaff` (uncommitted diff) |
| `masterProducts/{id}` | Used by `OcrReview.tsx` + `matching.ts` for catalog matching | Not queried directly | `read`: public (`true`); `write`: moderator or `data_entry` |
| `notifications/{id}` | Created in `Notifications.tsx` with `status: 'sent'` | Read in `ConsumerPortal.tsx` (web) | `read, create, update, delete`: **`true` (public)** |
| `fcmTokens/{token}` | Queried in `Notifications.tsx` (`status == 'active'`) | Written by `FirestoreDealFinderDao.saveFcmToken` and `MyFirebaseMessagingService.onNewToken` | `read, create, update`: **`true` (public)**; `delete`: admin |
| `notificationDeliveries/{id}` | Created in `Notifications.tsx` with `status: 'sent'` | Not updated by Android FCM service | `read, create, update`: **`true` (public)**; `delete`: admin |
| `analytics/{id}` | Written by `analytics.ts` (`trackEvent`), read in `AdminAnalytics.tsx` | `AnalyticsManager.kt` present | `read, create`: **`true` (public)**; `update, delete`: admin |
| `auditLogs/{id}` | Appended by `firebase-hooks.ts` (`logAction`), read in `AuditLogs.tsx` | Not used | `read`: admin; `create`: `isSignedIn()`; `update, delete`: `false` |

---

## 4. Known Documentation vs. Code Discrepancies

1. **`user-app/DEPENDENCY_MAP.md`** states flyers/products are filtered by `status == "approved"`, whereas `FirestoreDealFinderDao.kt` and `DealFinderViewModel.kt` filter for `status == "published"`.
2. **`merchant-app/DEPLOY_HOSTING.md`** states `@google/genai` is unused and OCR only uses `tesseract.js`, whereas `merchant-app/src/lib/flyerExtractor.ts` actively calls `@google/genai` (`gemini-2.5-flash`) from the browser when a key is present.
3. **`merchant-app/SECURITY_REPORT.md`** and **`user-app/SECURITY_REPORT.md`** claim the Firestore/Storage rules are production-hardened and enforce strict role isolation, whereas the actual `merchant-app/firestore.rules`, `merchant-app/storage.rules`, and `user-app/storage.rules` still contain critical P0 vulnerabilities documented in `docs/security/FINDINGS.md`.
