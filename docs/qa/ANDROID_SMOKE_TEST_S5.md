# Phase S5 — Android Shopper Demo & Smoke Test Script

**Scope:** Verify the end-to-end Android consumer journey against a staging Firebase backend (never production `gen-lang-client-0615495350`) with no unlabelled placeholder data.

## Pre-requisites
- Debug APK built from `phase/s5-android-ux-queries` (`.\gradlew.bat :app:assembleDebug`).
- Staging Firestore named database (`FirebaseManager.FIRESTORE_DATABASE_ID`) seeded with at least 2 published retailers in Dubai, 1 published flyer, 1 active offer with verified branch coordinates, and 1 offer without verified branch coordinates.
- Physical Android device (or emulator) with Airplane Mode toggle and Location Settings accessible.

## Smoke Test Checklist

1. **Fresh Install & Permissions**
   - Install APK cleanly (`adb install -r app-debug.apk`), launch app.
   - Verify first-open Home screen renders cleanly without fake demo rows, fake `-20%` badges, `"Philippines"` brand fallbacks, or `"Within 10 km"` claims.
   - Verify current Emirate pill is visibly shown in the header and section subtitles.

2. **Exact Product Search & Offer Provenance**
   - Tap Search / Compare, confirm suggestions are labeled **Suggested Searches** (not "Popular" or "Trending").
   - Search for an exact published product (e.g. `Basmati Rice 5kg`).
   - Open the product detail screen and verify:
     - Accurate AED price and optional strikethrough old price (only when `oldPrice > currentPrice`).
     - **Source** (`Retailer flyer / authorized feed` or explicit `sourceLabel`) and **Last confirmed** timestamp/status are displayed.
     - Unit/pack size, validity date, and promo/loyalty terms are displayed when present.
   - Search for a nonsense string (`zzzz-no-such-item-999`) and confirm an honest `"No matching offers found"` state is shown (no silent fallback to unrelated products).

3. **Smart Basket Arithmetic & Comparison**
   - Add 2+ products to the Shopping List, adjust quantities with `+`/`-`, and open the Smart Basket optimizer.
   - Compare **1-Store** vs **2-Store** totals against a hand calculator (integer-fils AED arithmetic).
   - Confirm incomplete store coverage shows coverage fraction (`X/Y items`) without fabricating a total.

4. **Background / Reopen Persistence**
   - Background the app (`Home`), wait 5 seconds, reopen from Recents.
   - Verify shopping list items, quantities, and selected Emirate persist and navigation remains healthy.

5. **Offline / Cached Mode Policy**
   - Enable Airplane Mode and relaunch or refresh the app.
   - Confirm Home and Compare clearly display the **Offline Mode — Cached Data** / **Data may be outdated** warning banner (never claiming live retailer prices).
   - Confirm expired cached flyers are still hidden/excluded while offline.
   - Disable Airplane Mode and confirm connectivity callback refreshes data and clears the offline banner once server sync succeeds.

6. **Merchant Staging Publish -> Consumer Visibility**
   - Publish a new flyer/offer in the merchant portal (staging) with an approved image and source label.
   - Tap **Refresh** on Home (or wait for snapshot listener) and confirm the consumer app displays the newly published offer with accurate image and source label.

7. **Merchant Withdraw / Offer Expiry**
   - Withdraw the offer (or set `validTo` in the past) in staging.
   - Confirm the offer disappears from Home, Search, Compare, and Smart Basket active pricing, and is not priced as an active deal in saved lists.

8. **Denied GPS -> Manual Emirate Selection**
   - Deny Location permission when prompted on the Location screen.
   - Manually pick an Emirate (e.g., `Sharjah` or `Abu Dhabi`).
   - Confirm stores/flyers/offers update to the chosen Emirate, no false map pin is shown, and branches without verified coordinates show `"Distance unavailable"` / `"Branch directions unavailable"` instead of fake distances.
   - For a branch *with* verified coordinates (`coordsVerified == true`), tap **Directions** and confirm the `geo:` maps intent opens with the exact coordinates.

9. **Arabic Locale & RTL**
   - Switch app language to **العربية** in Account → Settings.
   - Confirm RTL layout renders cleanly, mixed English brand + Arabic text remains legible, and Arabic search queries are not stripped by normalization.
