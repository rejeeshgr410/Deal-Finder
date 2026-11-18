# Schema V2 Migration & Compatibility Specification

**Version:** `2`  
**Date:** 18 November 2026  
**Target Named Firestore Database:** `ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096` (never `(default)`)

---

## 1. Design Principles

1. **Additive First:** V2 fields are added alongside V1 fields (`currentPrice`, `oldPrice`, `unitSize`, `unitType`, `validFrom`, `validTo`, `status`) so existing Android (`user-app`) and merchant web (`merchant-app`) clients continue reading and deserializing documents without crashes.
2. **Never Invent Provenance or Coordinates:** V1 records without verified permission records are classified `verificationLabel: "unverified"` / `rightsStatus: "UNVERIFIED"`, never backfilled with guessed approval timestamps. Branch coordinates created without a verified source are marked `coordsVerified: false`.
3. **Price Semantics Separation:**
   - `amountMinor`: integer fils (`1 AED = 100 fils`). Authoritative for V2 arithmetic.
   - `comparisonPriceMinor` (maps from legacy `oldPrice`): represents **printed comparative / strike-through price only**, NOT an independently observed historical price.
   - Multibuy/loyalty rules (`minUnits`, `buyUnits`, `freeUnits`, `loyaltyRequired`, `couponRequired`, `promoConditions`) are stored explicitly so unit prices are never miscalculated.
4. **UTC Storage:** `validFromIso` and `validToIso` store UTC ISO-8601 strings alongside epoch-millisecond `validFrom` / `validTo` for V1 compatibility.

---

## 2. Field Mapping (V1 -> V2 Additive)

### `products/{productId}` (PublicOfferV2)
| V1 Field | V2 Additive Field(s) | Notes / Transform |
|---|---|---|
| `currentPrice` (`number` AED) | `amountMinor` (`int` fils), `currency: "AED"` | `Math.round(currentPrice * 100)` |
| `oldPrice` (`number \| null`) | `comparisonPriceMinor` (`int \| null`) | `oldPrice > 0 ? Math.round(oldPrice * 100) : null` |
| `unitSize`, `unitType` | `baseQuantity`, `baseUnit` (`g \| ml \| each`), `packCount` | Canonical SI conversion (`kg->g`, `L->ml`); pack counts preserved |
| `barcode` | `barcodeGtin` | Only populated if GS1 mod-10 check digit passes |
| `validFrom`, `validTo` (ms) | `validFromIso`, `validToIso` (UTC ISO) | Epoch ms preserved for V1 Android queries |
| `status` | `status`, `schemaVersion: 2` | Normalized across `draft`, `extracting`, `review_required`, `approved`, `published`, `rejected`, `expired`, `withdrawn` |
| *(none)* | `sourceType`, `sourceLabel`, `verificationLabel`, `availability`, `isDemo`, `dedupeKey`, `importRunId` | Public provenance summary; `availability` defaults to `"unknown"` |
| *(none)* | Internal `provenance` (`sourceAssetId`, `sourcePage`, `sourceBox`, `permissionBasisRef`, `modelName`, `modelTemplateRevision`, `rightsStatus`) | Protected audit/rights fields; `permissionBasisRef` never shown in consumer UI |

### `branches/{branchId}`
| V1 Field | V2 Additive Field(s) | Notes |
|---|---|---|
| `latitude`, `longitude` | `coordsVerified` (`boolean`), `coordSource` (`string`) | `(0,0)`, out-of-UAE coords, and `seed_d4d_data.ts` random coords marked `coordsVerified: false` and excluded from consumer distance math |

---

## 3. Migration Execution & Rollback Protocol

1. **Pre-flight Audit (Read-Only):**
   ```bash
   npx tsx scripts/audit-data.ts --json-snapshot <path>
   npx tsx scripts/validate-source-rights.ts
   ```
2. **Backup:** Export target named Firestore database to GCS/local backup file before any live write.
3. **Dry-Run Migration (Default):**
   ```bash
   npx tsx scripts/migrate-v2.ts --input <snapshot.json>
   ```
   Outputs record counts, SHA-256 checksums of planned patches, quarantined synthetic records, and `UNVERIFIED` classifications with **zero writes**.
4. **Apply (Staging First, Explicit Gate):**
   Requires `--apply`, `--confirm-project <projectId>`, and `--backup-file <verified-backup-path>`. Refuses production `gen-lang-client-0615495350` unless `--allow-production-migration` is explicitly supplied by the owner.
5. **Rollback:**
   - Because V2 is strictly additive, V1 fields remain intact and older clients keep working.
   - Each migration run records `migrationRunId` and writes a reversible patch log (`preImage` of modified keys) so reverting `schemaVersion` / removing additive fields or restoring from backup is deterministic.
