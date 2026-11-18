# Deal Finder UAE — Demo Asset Provenance Register

**Policy:** Every flyer PDF, retailer logo, product image, and promotional price displayed in a live demo, recorded backup, or competition slide deck must be logged in this table with its lawful source and permission basis. Unverified or scraped third-party assets are prohibited.

---

## Asset Register

| Asset ID | Asset Type (Flyer / Image / Price / Logo) | Retailer / Brand | Source URL or Authorized Document | Permission Basis (`retailer_authorized` / `public_licensed` / `synthetic_demo_labeled`) | Capture / Verification Date | Expiry Date (`validTo`) | Owner Sign-off |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `DEMO-001` | Benchmark Annotation Fixture | Synthetic UAE Grocery Mix (30 rows) | `test-fixtures/annotations/uae_flyer_benchmark_v1.json` | `synthetic_demo_labeled` (internal extraction QA only; not for shopper claims) | 2026-11-18 | N/A | Engineering |
| *Add staging demo flyer rows before judging* | — | — | — | — | — | — | — |

---

## Pre-Demo Provenance Checklist
- [ ] Every flyer shown in the 90-second demo has a row above with a non-expired `validTo` date or explicit `DEMO / SAMPLE` labeling.
- [ ] No retailer logo is presented in a way that implies an official partnership or endorsement unless written permission is attached.
- [ ] Branch coordinates used for `"Directions"` in the demo are verified (`coordsVerified == true`, `coordSource == 'verified_branch_source' | 'merchant_entered' | 'official_feed'`).
- [ ] Any recorded backup video uses the exact same dataset documented above and is explicitly announced as a recording if played.
