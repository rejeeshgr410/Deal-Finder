# Deal Finder UAE — Ground-Truth Validation Protocol & Results

**Status:** Protocol finalized (Phase S7); field sessions **not yet executed**. Record only consented, directly observed sessions below.

---

## Part A — Shopper 8-Item Grocery Comparison Protocol

### Target Sample
- **Target:** 10–20 volunteer UAE households (recruited with written/recorded consent; store anonymous participant IDs `P01..P20` only, no personal email/phone in results).

### Fixed Test Brief
1. Fix **Date**, **Emirate** (e.g., Dubai or Sharjah), **Eligible Retailers** (minimum 2 active staging retailers with verified flyers), and **8 Exact Grocery Items** with explicit brand/unit/pack size (e.g., `Almarai Fresh Milk Full Cream 1L`, `Tilda Basmati Rice 5kg`, `Eggs 30 pcs`, etc.).
2. Record the **ground-truth offer source and expiry date** for every item in both retailers before the session.
3. **Baseline Task:** Ask the participant to compare the 8-item basket using their usual method (PDF flyers / retailer websites). Measure completion time (seconds) and chosen store(s).
4. **Deal Finder Task:** Ask the participant to enter the same 8 items into Deal Finder Smart Basket, review 1-store vs 2-store options, and explain *why* the app recommended its top option.
5. **Observe & Log:**
   - Task completion (Yes/No) and elapsed seconds.
   - Exact SKU matching errors observed (count).
   - Missing/unpriced items and whether the participant understood the coverage disclosure.
   - Whether the participant could accurately explain the recommendation (Yes/No).
   - Post-test reuse interest (1–5 Likert scale + verbatim consent-approved quote).
6. **Claim Restriction:** Do **not** claim *"actual AED saved"* unless supported by a verified receipt/checkout outcome.

### Shopper Session Log (Populate During Pilot)
| Participant ID | Date | Emirate | Baseline Time (s) | Deal Finder Time (s) | Completed? | SKU Match Errors | Explained Why? | Reuse Interest (1–5) | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| *No sessions recorded yet* | — | — | — | — | — | — | — | — | — |

---

## Part B — Merchant Flyer Workflow & Interview Protocol

### Target Sample
- **Target:** 2–3 UAE grocery/supermarket managers or flyer coordinators, plus 1 authorized multi-page flyer timed end-to-end.

### Measurement Steps
1. **Manual Baseline:** Record total start-to-approved minutes for a human operator to manually enter all offers from an authorized flyer into the portal.
2. **AI-Assisted + Review Workflow:** Process the **exact same flyer** through `createFlyerExtractionJob` → `OcrReview` → `publishFlyer`.
3. Record separately:
   - Upload & AI extraction time (seconds)
   - Human review & correction time (minutes)
   - Total end-to-end time (minutes) — **never omit review time**
   - Total rows extracted, rows corrected (by field: price, unit, condition, identity), final field accuracy vs ground truth, and API/cloud cost estimate.
4. Ask open-ended interview questions on current flyer preparation workflow, pain points, pilot willingness, and pricing expectations. Record direct wording only with explicit consent.

### Merchant Workflow Benchmark Log (Populate During Pilot)
| Flyer ID / Source | Pages | Manual Entry Time (min) | AI Extraction Time (s) | Human Review Time (min) | Total AI+Review Time (min) | Rows Corrected / Total | Final Field Accuracy | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| *No pilot flyer timed yet* | — | — | — | — | — | — | — | — |
