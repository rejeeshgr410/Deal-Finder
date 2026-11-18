# Demo vs. Production Data & UI Audit

**Inspected:** 18 November 2026  
**Purpose:** Identify every screen, component, script, and dataset that currently mixes synthetic/hardcoded/demo data with live production paths so none are misrepresented to users, merchants, or judges.

---

## 1. Merchant Portal (`merchant-app/`)

| Surface / File | Observed Demo / Hardcoded Behavior | Classification | Required Action |
|---|---|---|---|
| `src/pages/Dashboard.tsx` | Hardcoded `performanceData` (Jan–Jun views/searches/clicks) on lines 8–15; retailer card `"Total Views (Est.)"` hardcoded to `'1,240'` (line 49); CSV export outputs `performanceData`. | Hardcoded / Synthetic | Replace with real aggregated events or hide behind an explicit `DEMO / SAMPLE` banner excluded from KPIs (Phase S6). |
| `src/pages/OcrAnalytics.tsx` | Hardcoded `accuracyData` (W1–W6, 82%–94%), `errorTypes`, and KPI cards (`94.2%`, `5.8%`, `12.4k`, `88.5%`) with zero Firestore queries. | 100% Hardcoded | Replace with real audited job/benchmark telemetry or mark `DEMO / SAMPLE` (Phase S3/S6). |
| `src/pages/RetailerAnalytics.tsx` | Hardcoded `performanceData` (Jan–Jun), KPI cards (`45.2k`, `12.1k`, `89k`, `4.5k`), and hardcoded `Top Performing Flyers` list. | 100% Hardcoded | Replace with tenant-scoped backend aggregates or explicit `DEMO / SAMPLE` state (Phase S6). |
| `src/pages/AdminAnalytics.tsx` | Queries `analytics`, `users`, `retailers` (`useCollection`), **but** mixes in hardcoded `viewsByEmirate`, `activityOverTime`, `topSearchTerms` backfill, fallback totals (`45` users, `24` views, `18` clicks, `12` saves), hardcoded `+12.5%` etc. badges, and fallback retailer list. | Mixed Live + Hardcoded Fallback | Remove all hardcoded fallbacks and mock chart arrays; show empty/zero state when no verified events exist (Phase S6). |
| `src/pages/Notifications.tsx` | Writes `status: 'sent'` to `notifications` and `notificationDeliveries` without invoking FCM Admin SDK. | Simulated Dispatch | Replace with trusted backend FCM dispatch and truthful provider status states (Phase S6). |
| `src/lib/flyerExtractor.ts` | Hardcodes `confidence: 96` (Gemini), `78` (Tesseract), `65` (grid fallback). | Arbitrary Confidence | Replace with structured validation flags and measured benchmark metrics (Phase S3). |
| `src/scripts/seed_d4d_data.ts` | Seeds `D4D` scraped retailers/flyers/products with `Math.random()` branch lat/lon, synthetic phone numbers, and random `views`/`clicks`. | Seed / Synthetic Data | Restrict to isolated staging with `sourceType: 'demo'`; audit and quarantine from production (Phase S1/S2). |

---

## 2. Consumer Android App (`user-app/`)

| Surface / File | Observed Demo / Hardcoded Behavior | Classification | Required Action |
|---|---|---|---|
| `ui/lists/ListScreen.kt` | When `displayItems.isEmpty()`, displays 4 hardcoded `DemoListItem` rows (`Basmati Rice 5kg`, `Fresh Milk 1L`, `Eggs (30 pcs)`, `Chicken 1kg` with hardcoded prices/retailers), hardcoded `BudgetCard` (`AED 500.00`, `AED 265.50 spent`, `AED 42.30` saved), and hardcoded `BestBasketPriceCard` (`At Nesto Hypermarket`, `8 items`). | Hardcoded Fallback UI | Show genuine empty state; optional `"Load sample basket"` only behind explicit `DEMO` label and excluded from analytics (Phase S4/S5). |
| `ui/lists/BasketOptimizationEngine.kt` | Estimates missing items using `item.targetPrice`; substitutes `2.5 km` or `singleStoreDistance + (N * 1.5)` when branch distance is missing; computes savings against `originalCost` (`targetPrice`). | Estimated / Misleading Math | Mark incomplete baskets `totalFils = null`, omit unverified distances, and use defensible savings baselines (Phase S4). |
| `ui/compare/CompareScreen.kt` | Falls back to `allProducts.take(10)` if filtered results are empty; displays `distanceKm ?: 1.2`; computes missing `oldPrice` as `currentPrice * 1.25`; falls back store name to `"Nesto Hypermarket"`. | Hardcoded Fallbacks | Render honest empty search results, `"Distance unavailable"`, omit `oldPrice` if absent, and show `"Unknown retailer"` if store missing (Phase S4/S5). |
| `ui/DealFinderViewModel.kt` | `trendingSearches` is a hardcoded `flowOf(...)` list of 6 grocery queries (lines 82–84). | Hardcoded Suggestions | Label as `"Suggested searches"` or back with privacy-safe measured search counts (Phase S5). |
| `app/src/main/assets/flyers/` & `D4D/` | Bundled D4D flyer JPEG assets and scraped catalog JSONs. | Unlicensed / Scraped Assets | Audit source permissions; isolate from production unless licensed/authorized (Phase S2). |

---

## 3. Target Separation Policy (Enforced Starting S1/S2)

1. **Staging vs. Production:** Seed scripts and demo datasets may only target an isolated staging Firebase project/database, never production `gen-lang-client-0615495350` without explicit owner authorization and `demo` isolation flags.
2. **Environment Tagging:** Analytics events and public offers must carry explicit `environment` / `sourceType` tags so demo traffic never appears in business or competition KPIs.
3. **Zero Silent Fabrication:** Missing prices, missing distances, missing `oldPrice`, and empty search results must render explicit unknown/empty states rather than hardcoded defaults.
