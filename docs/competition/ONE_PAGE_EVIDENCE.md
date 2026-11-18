# Deal Finder UAE — One-Page Evidence Board (Phase S7)

**Date:** 2026-11-19  
**Repository State:** Phases S0–S6 implemented on sequential feature branches (`phase/s0-baseline` through `phase/s6-notifications-analytics`), production Firebase (`gen-lang-client-0615495350`) untouched.

> **Integrity Rule:** Every number below is either directly measured from automated tests/builds in this repository or explicitly marked **PENDING PILOT EXECUTION**. Zero invented users, savings, partnerships, or survey results.

---

## 1. Verified Engineering & Quality Evidence (Measured)

| Capability | Verification Method | Measured Outcome | Denominator / Scope | Date |
| --- | --- | --- | --- | --- |
| **Smart Basket 2.0 & Android Core** | `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks :app:assembleDebug :app:lintDebug` | **33 / 33 passed** (0 failed, 0 skipped); APK & Lint pass | 10 JUnit suites (integer-fils math, SKU matcher, eligibility, 1-vs-2 store optimizer, query/cache policy, V2 contract) | 2026-11-19 |
| **Backend Cloud Functions v2** | `npm run lint && npm run build && npm test` in `firebase/functions/` | **26 / 26 passed** (0 failed, 0 skipped); `tsc` clean | 5 Vitest suites (auth/RBAC, Schema V2, atomic flyer publish/withdraw, FCM truth model, PII-stripped analytics) | 2026-11-19 |
| **Merchant Portal Web App** | `npx tsc --noEmit && npm run build` in `merchant-app/` | **Build passed** (0 TS errors; `vite build` 8.71s) | Full portal including OCR review gating, server-side FCM dispatch, and honest operational analytics | 2026-11-19 |
| **Security Rules Parity** | Canonical `firebase/firestore.rules` mirrored to `merchant-app/` and `user-app/` | **SHA-256 identical** across all 3 copies; public writes blocked on `notifications`, `notificationEvents`, `analytics`, `extractionJobs` | Verified via Emulator negative tests (Phase S1) + SHA-256 check (Phase S6) | 2026-11-19 |
| **Extraction Benchmark Harness** | `scripts/benchmark-extraction.ts` against `test-fixtures/annotations/uae_flyer_benchmark_v1.json` | **30-row annotated UAE fixture** ready for repeatable accuracy runs | 30 labeled grocery rows (English/Arabic, packs, BOGO, loyalty) | Phase S3 |

---

## 2. Field Validation & Pilot Evidence (Targets — Not Yet Executed)

| Validation Track | Target Sample Size | Measured Result | Status |
| --- | --- | --- | --- |
| **Household 8-Item Grocery Comparison** | 10–20 volunteer households | *Pending execution* (see `VALIDATION_RESULTS.md`) | **NOT YET RUN** |
| **Merchant Workflow Interviews** | 2–3 UAE grocery/retailer managers | *Pending execution* | **NOT YET RUN** |
| **Manual vs AI-Assisted Flyer Timing** | 1 authorized multi-page retailer flyer | *Pending execution* | **NOT YET RUN** |
| **Retailer-Authorized Pilot Account** | 1 retailer-controlled staging tenant | *Pending outreach & written consent* | **NOT YET SECURED** |

---

## 3. Audit Trail
- **Architecture & Baseline:** `docs/qa/BASELINE.md`, `docs/architecture/EXISTING_DATA_FLOW.md`, `docs/qa/DEMO_VS_PRODUCTION.md`
- **Security & Privacy:** `docs/security/FINDINGS.md`, `docs/security/HISTORY_CLEANUP_PLAN.md`, `docs/security/PRIVACY_AND_RETENTION_POLICY.md`
- **Data Contract & QA:** `docs/migrations/SCHEMA_V2.md`, `docs/qa/ANDROID_SMOKE_TEST_S5.md`
