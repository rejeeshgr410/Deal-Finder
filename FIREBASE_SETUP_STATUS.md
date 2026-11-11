# Deal Finder UAE — Firebase Setup Status

_Last updated: 2026-06-07_

This document records the Firebase configuration work done to make the two apps
work together, and the remaining manual steps.

- **Consumer app (Android / Kotlin):** `user-app/` — package `com.aistudio.dealfinderuae.jlkqpz`
- **Business/admin app (React + Vite):** `merchant-app/`

---

## The core problem that was fixed

The two apps were pointing at **two different Firebase projects**, so no data could
flow between them. They are now unified on a single project.

**Shared Firebase project:** `gen-lang-client-0615495350` ("Default Gemini Project")
- Project number: `102162589559`
- Plan: **Blaze** (pay-as-you-go)
- Shared Firestore database (NAMED, not default):
  `ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096` (region: europe-west2)
- Storage bucket: `gen-lang-client-0615495350.firebasestorage.app` (region: US-EAST1)

> The old project `deal-finder-uae` (project number 818622531454) is now **abandoned**.
> It only ever had the Android app registered; it had no database/auth/storage.

---

## What was done

### Console
- [x] Registered the Android app in the shared project
      (App ID `1:102162589559:android:42483ed82f09de1bbfe372`).
- [x] Downloaded the new `google-services.json` and placed it at `user-app/app/`
      (old one backed up as `google-services.json.OLD-deal-finder-uae.bak`).
- [x] Enabled **Email/Password** authentication (Google was already enabled).
- [x] Upgraded the project to **Blaze** (required a one-time $10 prepayment — this is
      normal for UAE billing; it becomes prepaid credit).
- [x] Enabled **Cloud Storage**, created the default bucket, deployed corrected rules.
- [x] Removed the **AI shared-quota limit** on the Firestore database (full scaling).
- [x] Created a **$5/month budget alert** (emails at 50% / 90% / 100%).

### Code (Android)
- [x] `FirebaseManager.kt` — removed hardcoded **mock** credentials
      (`dealfinder-mock-123`). The app now auto-initializes from `google-services.json`
      and targets the NAMED Firestore database. **All** Firestore access goes through
      `FirebaseManager.getDb()`.
- [x] `MyFirebaseMessagingService.kt` — uses the shared named DB; FCM tokens now write
      to the **top-level `fcmTokens`** collection (matching what the business app reads).
- [x] `FirestoreDealFinderDao.kt` — same FCM token fix.

### Code / rules consistency
- [x] Confirmed the **business app's `firestore.rules`** is what's deployed
      (roles: `super_admin`, `admin`, `moderator`, `data_entry`, `retailer_owner`,
      `retailer_staff`; flyers visible when `status == 'published'`; field `retailerId`).
- [x] The Android query code matches this (`status == "published"`, `retailerId`,
      `retailers` collection) — so consumer ↔ business data is consistent in practice.
- [x] Replaced the **stale, never-deployed** `user-app/firestore.rules` with the
      canonical (deployed) rules. Old version saved as
      `user-app/firestore.rules.STALE-DO-NOT-DEPLOY.bak` — **do not deploy that file**;
      it describes a different, incompatible schema (`storeId`, `status=='approved'`).
- [x] Corrected `merchant-app/storage.rules` — the original used
      `firestore.get(/databases/(default)/...)`, which points at the WRONG (empty)
      database. Storage rules can only read `(default)`, so role lookups can't work.
      Writes are now gated on authentication + basic file validation. See the TODO in
      that file about hardening with Auth custom claims.

---

## Remaining manual steps (need your machine / decisions)

### 1. Build & run the Android app (true end-to-end test)
In Android Studio:
- Per the README, remove this line from `app/build.gradle.kts` before building:
  `signingConfig = signingConfigs.getByName("debugConfig")`
- Build & run on an emulator/device.
- **Test the data flow:** create a retailer/product/flyer in the business web app
  (sign in with `***REDACTED_EMAIL***` — that's the hardcoded super admin), then
  confirm it appears in the consumer app.

### 2. Google Sign-In (OPTIONAL — not wired up yet)
The Google Sign-In **button is not implemented** in the Android UI
(`AuthScreen.kt` has a comment where it was removed). Email/Password is the only
working login today. If/when you implement Google Sign-In (via CredentialManager),
you will then also need to add a **SHA-1 fingerprint**:

```bash
# Debug keystore SHA-1 (works on any machine without the Gradle wrapper):
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" \
  -alias androiddebugkey -storepass android -keypass android
# (On Mac/Linux: ~/.android/debug.keystore)

# OR, once Android Studio has generated the Gradle wrapper:
./gradlew signingReport
```
Copy the `SHA1:` value, then in Firebase Console →
Project Settings → Your apps → the Android app → **Add fingerprint**.
For a production release you must also add the **release** keystore's SHA-1.

### 3. Optional hardening
- Mirror user roles into **Auth custom claims** so the Storage rules can do real
  role checks (see TODO in `merchant-app/storage.rules`).
- Note the FCM subcollection vs top-level mismatch is now resolved, but verify push
  notifications end-to-end once you have a device token.

---

## Key IDs reference

| Thing | Value |
|---|---|
| Shared project ID | `gen-lang-client-0615495350` |
| Project number | `102162589559` |
| Android App ID | `1:102162589559:android:42483ed82f09de1bbfe372` |
| Web App ID | `1:102162589559:web:f1fe2c9a2f3551d3bfe372` |
| Firestore database (named) | `ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096` |
| Storage bucket | `gen-lang-client-0615495350.firebasestorage.app` |
| Billing account | `01F449-DF552D-E92254` (Active, Blaze) |
| Super admin email (hardcoded in rules) | `***REDACTED_EMAIL***` |
