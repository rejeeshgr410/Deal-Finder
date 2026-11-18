# Deal Finder UAE — Phase S0 Baseline Report

**Date:** 18 November 2026  
**Base Commit:** `4ee27a1` (`feat(ui): complete user app redesign matching 5 target mockup screens`)  
**Current Branch:** `phase/s0-baseline` (branched from `main`)  
**Uncommitted Pre-Existing WIP on Working Tree (Untouched in S0):** 22 modified files across `merchant-app/` (`capacitor.build.gradle`, `capacitor.settings.gradle`, `firestore.rules`, `package.json`, `package-lock.json`, `AuthContext.tsx`, `Promotions.tsx`) and `user-app/` (`build.gradle.kts`, `libs.versions.toml`, `MainActivity.kt`, `AuthRepository.kt`, `DatabaseEntities.kt`, `FirebaseManager.kt`, `FirestoreDealFinderDao.kt`, `DealFinderApp.kt`, `Screen.kt`, `AccountScreen.kt`, `AuthScreen.kt`, `AuthViewModel.kt`, `CompareScreen.kt`, `CouponsScreen.kt`, `firestore.rules`).

---

## 1. Environment & Tooling Inventory

| Tool / Runtime | Observed Version / Status |
|---|---|
| Node.js | `v26.3.0` |
| npm | `12.0.2` (`merchant-app/node_modules` present) |
| Java / JDK | OpenJDK `21.0.12+8-LTS` |
| Gradle | `9.7.0` (Kotlin `2.4.0`, Android Gradle Plugin via `libs.versions.toml`) |
| ADB | `1.0.41` (Version `37.0.1-15733141`); 2 attached Android devices observed (`26021FDF60020R`, `7ad79009`) |
| Firebase CLI (`firebase`) | **NOT INSTALLED** globally in PATH (`firebase` command not found; `npx firebase-tools` not pre-cached) |

---

## 2. Baseline Verification Commands Executed

All baseline commands were executed non-mutatingly against the current checkout:

| Component | Command | Outcome | Observed Details |
|---|---|---|---|
| Git status | `git status --short` & `git log -10 --oneline` | **PASS** | HEAD at `4ee27a1`; 22 pre-existing WIP modifications preserved |
| Merchant lint | `cd merchant-app && npm run lint` (`tsc --noEmit`) | **PASS** (exit 0) | Zero TypeScript errors |
| Merchant build | `cd merchant-app && npm run build` (`vite build`) | **PASS** (exit 0) | Built 2,440 modules in `16.98s` (`dist/assets/index-Cvna0Fip.js` 2,321.32 kB; chunk size warning only) |
| Merchant tests | `npm run test` | **NOT CONFIGURED** | No test runner or `test` script in `merchant-app/package.json` |
| Android unit tests | `cd user-app && .\gradlew.bat :app:testDebugUnitTest --rerun-tasks --console=plain` | **PASS** (exit 0) | 11/11 tests passed (`CrashDiagnosticsTest`: 1, `DealFinderFeatureTests`: 9, `ExampleUnitTest`: 1; 0 skipped, 0 failed; `finalizeTestRoborazziDebug` skipped) |
| Android debug APK + Lint | `cd user-app && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` | **PASS** (exit 0) | Debug APK built and `lintReportDebug` generated (`app/build/reports/lint-results-debug.html`) |
| Firebase live / emulator rules check | `firebase --version` / rules unit test | **BLOCKED** | `firebase` CLI and emulator test suite not yet installed/configured in repo |

> Note: `npm ci` was **not** executed destructively in S0 because `merchant-app/package-lock.json` contains pre-existing uncommitted WIP edits alongside `package.json`; `npm run lint` and `npm run build` were run directly against installed `node_modules`.

---

## 3. Confirmed vs. Uncertain Assumptions

### Confirmed from Code & Tool Output
1. **Shared Named Firestore Database:** Both `merchant-app/src/firebase.ts` (via `firebase-applet-config.json`) and `user-app/.../FirebaseManager.kt` target Firebase project `gen-lang-client-0615495350` and named database `ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096` (not `(default)`).
2. **No Trusted Backend Functions Yet:** There is no `functions/` directory or server-side Admin SDK endpoint in the repo; Gemini Vision (`flyerExtractor.ts`), publication (`OcrReview.tsx`), and notification logging (`Notifications.tsx`) all run directly in the browser.
3. **Rule Files State:** `merchant-app/firestore.rules` and `user-app/firestore.rules` currently match byte-for-byte (SHA256 `1D1AD667...`), whereas `merchant-app/storage.rules` and `user-app/storage.rules` differ in path structure, size limits, and role lookups.
4. **Verified Local Builds:** Both `merchant-app` (`tsc --noEmit` + `vite build`) and `user-app` (`:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`) succeed in this environment.

### Uncertain / Unverified (Requires Owner Input or External Access)
1. **Deployed Firebase Rules & Live Data:** What is currently deployed on `gen-lang-client-0615495350` (Firestore rules, Storage rules, Hosting build, and whether `seed_d4d_data.ts` synthetic rows exist in the live DB) is **NOT VERIFIED** because `firebase` CLI / Console access was not exercised in S0.
2. **Staging Environment:** No separate staging Firebase project/database ID is configured in `.firebaserc` or `firebase-applet-config.json`.
3. **Credential Rotation Status:** Tracked scripts in `merchant-app/src/scripts/` contain hardcoded passwords/emails; whether those accounts are active in Firebase Auth or reused elsewhere is **NOT VERIFIED**.
4. **Source Data Licensing:** Whether written retailer or feed permissions exist for any assets under `D4D/` or bundled `user-app/app/src/main/assets/flyers/` is **NOT VERIFIED**.

---

## 4. Application Feature Map (Observed)

- **Consumer Android App (`user-app/`):**
  - Entry: `MainActivity.kt` -> `DealFinderApp.kt` (6 bottom tabs: `Coupons`, `Home`, `Search/Compare`, `Lists`, `Deals/Flyers`, `Account`, plus `ProductDetail`, `FlyerViewer`, `Retailer`, `Location`, `Notifications`, `NotificationSettings`, `Auth`, `LoyaltyCards`).
  - Data: Room (`DealFinderRoomDatabase`) for shopping lists, cached flyers, loyalty cards; Firestore named DB via `FirestoreDealFinderDao` and `ShoppingListRepository` (`limit(100)` on core collection queries); FCM service in `MyFirebaseMessagingService.kt`.
  - Known gaps: Hardcoded demo list/budget cards in `ListScreen.kt`, target-price & distance fallbacks in `BasketOptimizationEngine.kt` and `CompareScreen.kt`, permissive `isIdenticalTo()`, no `validTo` filter on `DealFinderViewModel.products`.
- **Merchant / Admin Web App (`merchant-app/`):**
  - Entry: `App.tsx` + `AuthContext.tsx` (Email/Password + Google Sign-In + Capacitor Android social login).
  - Routes: `Dashboard`, `Flyers`, `OcrCenter`, `OcrReview`, `Products`, `Promotions`, `Retailers`, `Users`, `AuditLogs`, `Notifications`, `AdminAnalytics`, `RetailerAnalytics`, `OcrAnalytics`, `RetailerAccount`, `RetailerBranches`, `ConsumerPortal`.
  - Known gaps: Browser-resident Gemini API calls & `localStorage` key storage, hardcoded analytics charts/fallbacks, simulated FCM dispatch (`Notifications.tsx`), non-atomic `Promise.all` publish (`OcrReview.tsx`), hardcoded credentials in `src/scripts/`.

---

## 5. Next Safe Step

- Proceed to **Phase S1** only after owner confirms:
  1. How to handle the 22 pre-existing uncommitted WIP files (commit to a separate branch/stash or include in baseline).
  2. Dedicated **staging** Firebase project ID (or permission to set up local Firebase Emulator Suite tests first without touching production).
  3. Account password rotation for any exposed credentials referenced in `docs/security/FINDINGS.md`.
