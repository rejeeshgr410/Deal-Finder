# Deal Finder UAE — Judge Q&A Preparation

> **Rule:** Answer directly with verifiable engineering facts. Distinguish clearly between what is **built and tested today** vs **hypotheses to be validated in the pilot**.

---

### 1. Where do you get your price data, and do you have retailer permission?
- **Current state:** We do **not** scrape unauthorized websites or fabricate retailer partnerships. The architecture is built for two lawful ingestion paths: (a) retailer-uploaded flyers via our tenant-isolated Merchant Portal, and (b) authorized public/partner promotional feeds tagged with explicit `sourceType`, `sourceLabel`, and `lastVerifiedAt`.
- **Pilot limitation:** We have not yet signed a commercial retailer contract; our immediate next step (Phase S7) is onboarding 1 retailer-authorized pilot tenant and validating with lawful authorized flyers.

### 2. What happens when the AI OCR misreads a price or pack size?
- **No silent auto-publish:** Our Cloud Function pipeline (`createFlyerExtractionJob` → `OcrReview.tsx` → `publishFlyer`) never trusts raw model output directly. Rows missing valid prices, units, or expiry dates receive explicit `reviewReasons`, and the **Publish** button is gated until unresolved rows are reviewed and confirmed.
- **No fake confidence scores:** We removed hardcoded `confidence` percentages and track actual reviewer edits (`manual_correction` events) against a 30-row UAE benchmark (`uae_flyer_benchmark_v1.json`).
- **Instant withdrawal:** If an error slips through, `withdrawFlyer` atomically hides the flyer and its offers across consumer screens.

### 3. Why wouldn't a shopper just check D4D / ClickFlyer or Carrefour's own app?
- **Single-retailer apps** only show their own catalog. **Flyer aggregators** show static PDFs where shoppers still mentally compare 2×500 ml vs 1 L, BOGO rules, and multi-store baskets.
- **Our technical differentiation (Smart Basket 2.0):** Pure integer-fils arithmetic (`Money.kt`), strict descending-trust SKU equivalence (`GTIN → masterProductId → normalized brand + coreName + unit + pack + curated Arabic/English aliases`), explicit handling of loyalty/BOGO/min-units conditions, honest incomplete-coverage disclosure (`totalFils = null` instead of guessing), and 1-store vs 2-store comparison with item-level `"Why this option?"` explanations.

### 4. How do you protect consumer privacy and prevent fake analytics/notifications?
- **Firestore Security Rules:** Canonical rules (`firebase/firestore.rules`, byte-for-byte synced across all apps) deny all client writes to `analytics`, `notifications`, `notificationDeliveries`, `notificationEvents`, and `extractionJobs`.
- **PII minimization:** `trackAnalyticsEvent` hashes user IDs into a one-way salted `actorAnonymousId` and strips `email`, `phone`, and tokens server-side. `fcmTokens` and `notificationEvents` store no shopper emails.
- **Notification truth model:** Campaigns dispatch through Firebase Admin SDK (`sendNotificationCampaign`), enforce per-category opt-out server-side, and report `accepted_by_provider` separately from `client_received` / `opened` — never falsely claiming `delivered`.

### 5. What are your unit economics and AI/cloud costs?
- **Cost controls built in:** Gemini extraction runs strictly server-side behind authenticated role-checked Cloud Functions (never exposing API keys on clients), with bounded Firestore queries (100–500 doc caps) and Room local caching on Android.
- **Unvalidated hypothesis:** Exact cost-per-flyer-page and merchant willingness to pay (SaaS subscription vs featured placement) are **hypotheses** to be measured during the 2–3 merchant pilot interviews in `VALIDATION_RESULTS.md`.

### 6. Why did you not launch an AI chat assistant yet?
- **Discipline over hype:** An LLM assistant over unverified or incomplete prices hallucinates deals and destroys shopper trust. We intentionally kept P1 AI Assistant behind our P0 gates (security, Schema V2, human-reviewed extraction, deterministic Smart Basket 2.0, and pilot evidence) so any future assistant calls only our deterministic `BasketOptimizer` and verified catalog.
