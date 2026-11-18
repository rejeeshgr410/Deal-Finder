# Deal Finder UAE — 90-Second Winning Demo Script

**Goal:** Deliver a repeatable, auditable 90-second end-to-end demonstration using a staging environment seeded strictly with authorized or clearly labeled demo assets. Never present a pre-recorded backup as live.

---

## Pre-Flight Checklist (15 Minutes Before Slot)
1. **Staging Data Ready:** Merchant Portal (logged in as `admin` or `retailer` on staging) + Android device/mirror on the same staging named database (`FirebaseManager.FIRESTORE_DATABASE_ID`).
2. **Prepared Flyer Asset:** One authorized multi-page PDF/image flyer already verified in `DEMO_ASSET_PROVENANCE.md`, with one intentionally ambiguous row (e.g., missing unit size or loyalty condition) ready to demonstrate human-in-the-loop review.
3. **Android Device State:** Pre-selected Emirate (e.g. `Dubai`), clean Shopping List (empty or 2 pre-agreed items), notifications enabled, battery > 50%, Do Not Disturb enabled.
4. **Backup Video Ready:** Screen-recorded backup of the exact same build and dataset. **Rule:** If used, explicitly announce: *"Due to venue connectivity, I am playing a recording of this exact staging build."*

---

## Storyboard (90 Seconds)

| Time | Presenter Action | On-Screen System Proof | Talking Point |
| --- | --- | --- | --- |
| **0–10 s** | Show a multi-page UAE supermarket PDF flyer alongside scattered prices. | Authorized flyer source document. | *"UAE households compare weekly PDF flyers across hypermarkets manually — pack sizes, BOGO rules, and expiry dates make true basket comparison slow and error-prone."* |
| **10–28 s** | In **Merchant Portal**, upload the flyer and trigger extraction (`OcrCenter` → `OcrReview`). | Role-checked backend job created (`createFlyerExtractionJob`); server-side Gemini extraction runs with zero client API keys. | *"Merchants or editors upload a flyer once. Our server-side pipeline extracts structured offers with strict Schema V2 validation."* |
| **28–43 s** | In **OcrReview**, highlight an intentionally flagged row with `reviewReasons` (e.g., missing unit or loyalty requirement) and click **Next Unresolved**. | Pre-publish validation blocks publishing until the reviewer resolves flagged fields and clicks **Confirm Row**. | *"AI never publishes unchecked prices. Every ambiguous unit, expiry date, or promo condition is blocked for one-click human verification."* |
| **43–53 s** | Fix the flagged field, confirm row, and click **Publish Flyer**. | Atomic, idempotent `publishFlyer` Cloud Function writes published offers with `sourceLabel` and `lastVerifiedAt`. | *"Publishing is atomic and audited — merchants can withdraw an expired or mistaken flyer in one click."* |
| **53–75 s** | Switch to the **Android Consumer App**, open **Smart List**, add the exact grocery items, and view **1-Store vs 2-Store** comparison. | Integer-fils (`AED X.XX`) totals, strict SKU matching (no mixing 1 kg vs 5 kg), explicit coverage (`X/Y items`), `"Why this option?"` drivers, and `"Distance unavailable"` if branch coordinates are unverified. | *"Shoppers see exact unit-matched totals in fils — never guessed prices or fake discounts — and can compare a single trip against a two-store split."* |
| **75–84 s** | Tap a product to open **Product Detail**. | Shows **Source**, **Last confirmed**, pack/unit price, validity dates, and **Directions** (only for verified branch coordinates). | *"Every price shows its source, expiry, and verification date so shoppers trust what they see before driving to the store."* |
| **84–90 s** | Show `ONE_PAGE_EVIDENCE.md` / pilot ask slide. | Measured engineering metrics + transparent pilot status. | *"Our security, basket math, and human-in-the-loop pipeline are built and tested. We are now recruiting 10–20 UAE households and 1–2 supermarket partners for our staged pilot."* |
