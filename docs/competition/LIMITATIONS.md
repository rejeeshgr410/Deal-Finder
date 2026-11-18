# Deal Finder UAE — Current Limitations & Pilot Scope

Transparency about current gaps is mandatory for both internal engineering sign-off and competition judging.

## 1. Environment & Deployment Limitations
- **Production Firebase untouched:** All Phases S0–S7 were built and verified on local branches (`phase/s0-baseline` through `phase/s7-pilot-validation`). None of the new Cloud Functions (`firebase/functions/`), canonical Firestore/Storage rules (`firebase/firestore.rules`, `firebase/storage.rules`), or Schema V2 migrations have been deployed to a live staging or production Firebase project yet.
- **Git history secret cleanup pending:** Prior to making any repository public, the exposed legacy credentials identified in Phase S0/S1 must be rotated in Google Cloud / Firebase Console and purged from git history following `docs/security/HISTORY_CLEANUP_PLAN.md`.

## 2. Verification & Hardware Testing Limitations
- **Physical-device smoke test not yet executed:** Automated Android unit tests (33/33 passed), `:app:assembleDebug`, and `:app:lintDebug` passed, and Functions unit tests (26/26 passed) + Merchant Portal build passed. However, `:app:finalizeTestRoborazziDebug` was skipped, and the physical-device shopper smoke test (`docs/qa/ANDROID_SMOKE_TEST_S5.md`) and live end-to-end FCM push delivery require a deployed staging Firebase project and real Android hardware.
- **Account deletion UI wiring:** The `requestAccountDataDeletion` Cloud Function is implemented and built in `firebase/functions/src/privacy.ts`, but wiring a confirmation button in Android `AccountScreen.kt` is deferred until staging Cloud Functions deployment is live.

## 3. Data, Content & Commercial Limitations
- **No signed retail partnerships yet:** No supermarket chain has signed an LOI or commercial agreement yet. All retailer logos/names in local test fixtures are strictly for technical testing and must not be displayed as official endorsements.
- **Extraction benchmark scope:** `test-fixtures/annotations/uae_flyer_benchmark_v1.json` currently contains 30 annotated UAE grocery rows; accuracy on live multi-column bilingual retailer PDFs must be re-measured with authorized retailer flyers during Phase S7 pilot execution.
- **Field pilot not yet executed:** Household 8-item comparison sessions (target: 10–20 households) and merchant interviews (target: 2–3 managers) have finalized protocols (`VALIDATION_RESULTS.md`) but have not yet been run.
- **P1 Differentiators intentionally deferred:** Grounded AI Shopping Assistant (Phase P1-A) and further acceleration features remain unbuilt until P0 staging deployment and real pilot feedback are completed.
