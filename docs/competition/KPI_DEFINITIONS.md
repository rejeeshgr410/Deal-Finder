# Deal Finder UAE — KPI Definitions (Zero Invented Numbers)

Every metric displayed in Merchant Portal dashboards, pilot reports, or competition slides must strictly match the formula, source collection, and exclusion rules below.

| KPI Name | Exact Definition / Formula | Data Source | Exclusions & Guardrails |
| --- | --- | --- | --- |
| **Active Published Offers** | Count of `products` where `status == 'published'`, `availability != 'out_of_stock'`, `amountMinor > 0` (or `currentPrice > 0`), `validFrom <= now`, and `validTo > now` (or `validTo == 0` for V1 legacy). | `products` collection | Exclude `isDemo == true`, `sourceType == 'demo'`, draft, expired, or withdrawn items. |
| **Offers Expiring < 48h** | Count of Active Published Offers where `0 < validTo - now <= 48h`. | `products` collection | Computed dynamically at query time. |
| **Verified Flyer Views** | Count of `analytics` documents where `eventName == 'flyer_view'` within selected date range. | `analytics` (via `trackAnalyticsEvent`) | Exclude `environment == 'demo'` and `isDemo == true`; no client direct writes allowed. |
| **Product Clicks** | Count of `analytics` documents where `eventName == 'product_click'` (intentional tap on a product card/offer). | `analytics` (via `trackAnalyticsEvent`) | Exclude demo events; never extrapolate revenue or purchase conversion. |
| **Basket Comparisons Completed** | Count of `analytics` documents where `eventName == 'basket_comparison_completed'` (user actually viewed computed 1-store vs 2-store result). | `analytics` (via `trackAnalyticsEvent`) | Exclude demo events and empty-list opens. |
| **Median Upload-to-Approval Time** | Median `(publishedAt - createdAt) / 60000` (minutes) across published `extractionJobs`. | `extractionJobs` collection | Requires both `createdAt` and `publishedAt` server timestamps; shows `N/A` if denominator = 0. |
| **Manual Correction Rate** | `count(manual_correction events) / total extracted rows reviewed` broken down by field (`price`, `unit`, `condition`, `identity`). | `analytics` (`manual_correction`) + `extractionJobs` | Never substitute hardcoded model confidence percentages. |
| **FCM Provider Acceptance Rate** | `acceptedByProvider / matchedTokens` for a campaign in `notifications`. | `notifications` + `notificationEvents` | **Must be labeled "Provider Accepted"**, never "Delivered" (FCM acceptance ≠ device delivery). |
| **Client Receipt / Open Rate** | `clientReceived / acceptedByProvider` and `opened / acceptedByProvider` where Android client acknowledged receipt/open via `acknowledgeNotificationEvent`. | `notifications` + `notificationEvents` | Subject to Android background/Doze limitations; always state denominator. |
| **Pilot Participant Count** | Count of verified, consented household/merchant participants logged in `VALIDATION_RESULTS.md` (or `pilot_test` events). | Pilot log / `analytics` (`pilot_test`) | Never count anonymous app installs or developer test runs as pilot participants. |
