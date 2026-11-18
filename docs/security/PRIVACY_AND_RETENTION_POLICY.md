# Deal Finder UAE — Privacy, Consent & Data Retention Policy (Phase S6)

**Status:** Draft for staging & pilot review (aligned to actual codebase collection and Firebase processors as of Phase S6).

## 1. Data Collected & Minimization
- **Account Profile (`users/{uid}`):** Display name, authentication identifier, role (`consumer`, `retailer`, `admin`), selected notification category preferences (`notifyNewFlyers`, `notifyPriceDrops`, `notifyDealExpiry`, `notifyPromotions`, `notifyCouponExpiry`).
- **Shopping Lists & Saved Items (`users/{uid}/{subcollection}`):** User-owned saved deals, flyers, coupons, followed retailers, shopping list items, and recent search queries. Strictly owner-scoped via Firestore Security Rules.
- **Push Registration Tokens (`fcmTokens/{token}`):** Token ID, owning `userId`, `deviceType`, `status`, `updatedAt`, and optional `emirate` / `followedRetailers` targeting metadata. **No shopper email address or phone number is stored on `fcmTokens` or `notificationEvents`.**
- **Analytics (`analytics/{eventId}`):** Server-validated events (`flyer_view`, `product_click`, `search`, `coupon_save`, `retailer_page_view`, `promotion_click`, `basket_comparison_completed`, `merchant_offer_published`, `ai_extraction_completed`, `manual_correction`, `pilot_test`) carrying a salted one-way SHA-256 `actorAnonymousId`. All PII fields (`email`, `userEmail`, `phone`, `phoneNumber`, `token`, `fcmToken`, `password`, `address`, `fullName`) are stripped server-side before storage.
- **Location:** GPS coordinates are used on-device solely to select the nearest UAE Emirate and compute straight-line branch distance. Raw GPS coordinates are never written to public analytics or shared with merchants.

## 2. Processors
- **Google Firebase Authentication:** Account identity and session management.
- **Google Cloud Firestore (named database):** Structured application storage with role- and owner-scoped security rules.
- **Firebase Cloud Messaging (FCM):** Server-side push dispatch via Firebase Admin SDK.
- **Google Gemini API (server-side Cloud Functions only):** Flyer image extraction for merchant-uploaded PDFs/images; no consumer PII is sent to Gemini.

## 3. Notification Consent & Truth Semantics
- Promotional/marketing notifications require opt-in; users may opt out per category or unsubscribe from all categories in the Android app (**Account → Notification Preferences**).
- Backend campaign dispatch (`sendNotificationCampaign`) checks user preferences prior to sending and skips opted-out users.
- Provider acceptance (`accepted_by_provider`) is never misrepresented as confirmed device delivery.

## 4. Retention Schedule
| Data Category | Retention Period | Notes / Cleanup Mechanism |
| --- | --- | --- |
| Active `fcmTokens` | Until logout, deletion, or 90 days inactive | Invalidated immediately when FCM returns `registration-token-not-registered` / `invalid-registration-token` |
| Raw flyer extraction images (Cloud Storage) | 30 days after job completion/publication | Retained briefly for QA/audit, then eligible for lifecycle purge |
| Extracted source snippets & review annotations | 180 days | Retained for versioned extraction quality benchmarking |
| Privacy-safe `analytics` events | 365 days | Contains only hashed `actorAnonymousId`, never raw PII |
| `notificationEvents` & `auditLogs` | 180 days (security/audit logs up to 365 days) | PII-free operational and compliance trail |

## 5. Account Data Export & Deletion
- Authenticated users can invoke `requestAccountDataDeletion` (Cloud Function) to delete their owned `fcmTokens`, `users/{uid}` subcollections (`savedDeals`, `savedFlyers`, `savedCoupons`, `followedRetailers`, `searchHistory`, `alerts`, `shoppingList`), and user profile document.
- A PII-free `account_data_deleted` audit record is written to `auditLogs` for compliance verification.
