import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  aedToFils,
  buildDedupeKey,
  isValidGtin,
  isValidSourceBox,
  mapV1ProductToV2Patch,
  normalizeQuantityUnit,
  validateBranchCoordinates,
  validateOfferForPublish
} from '../contracts/offerV2';
import { auditDataset } from '../../../../scripts/audit-data';
import { planV2ProductMigration } from '../../../../scripts/migrate-v2';
import { checkDemoIsolation } from '../../../../scripts/validate-demo-data';

describe('Schema V2 Offer Contract & Parity', () => {
  it('keeps firebase/functions offerV2.ts in sync with merchant-app offerContract.ts core types/validators', () => {
    const __dirname = path.dirname(fileURLToPath(import.meta.url));
    const repoRoot = path.resolve(__dirname, '..', '..', '..', '..');
    const fnContract = fs.readFileSync(path.join(repoRoot, 'firebase/functions/src/contracts/offerV2.ts'), 'utf8');
    const webContract = fs.readFileSync(path.join(repoRoot, 'merchant-app/src/lib/offerContract.ts'), 'utf8');
    expect(webContract).toContain('export function validateOfferForPublish');
    expect(fnContract).toContain('export function validateOfferForPublish');
    expect(webContract).toContain('export function validateBranchCoordinates');
    expect(fnContract).toContain('export function validateBranchCoordinates');
  });

  it('validates GTIN check digits and rejects guessed zeros', () => {
    expect(isValidGtin('5449000000996')).toBe(true);
    expect(isValidGtin('5449000000997')).toBe(false);
    expect(isValidGtin('0000000000000')).toBe(false);
    expect(isValidGtin('abc123')).toBe(false);
  });

  it('normalizes canonical units and preserves pack counts', () => {
    expect(normalizeQuantityUnit(1.5, 'L')).toEqual({ baseQuantity: 1500, baseUnit: 'ml', packCount: 1 });
    expect(normalizeQuantityUnit(2, 'kg')).toEqual({ baseQuantity: 2000, baseUnit: 'g', packCount: 1 });
    expect(normalizeQuantityUnit('6x330', 'ml')).toEqual({ baseQuantity: 330, baseUnit: 'ml', packCount: 6 });
    expect(normalizeQuantityUnit(0, 'kg')).toBeNull();
    expect(normalizeQuantityUnit(1, '')).toBeNull();
  });

  it('validates UAE branch coordinates and rejects (0,0) and out-of-UAE points', () => {
    expect(validateBranchCoordinates(25.1181, 55.2007, 'merchant_entered')).toEqual({
      valid: true,
      latitude: 25.1181,
      longitude: 55.2007,
      coordsVerified: true,
      errors: []
    });
    expect(validateBranchCoordinates(25.1181, 55.2007, 'unverified_seed').coordsVerified).toBe(false);
    expect(validateBranchCoordinates(0, 0, 'merchant_entered').valid).toBe(false);
    expect(validateBranchCoordinates(40.7128, -74.006, 'merchant_entered').valid).toBe(false);
  });

  it('enforces pre-publish gates (expiry, 0 price, guessed default box, verification receipt, BOGO)', () => {
    const now = 1_700_000_000_000;
    expect(isValidSourceBox([100, 100, 400, 400])).toBe(false); // guessed fallback box rejected
    expect(isValidSourceBox([10, 20, 250, 300])).toBe(true);

    const validResult = validateOfferForPublish(
      {
        retailerId: 'carrefour-uae-259',
        nameEn: 'Almarai Fresh Milk 2L',
        amountMinor: aedToFils(10.5),
        comparisonPriceMinor: aedToFils(12.5),
        baseQuantity: 2000,
        baseUnit: 'ml',
        packCount: 1,
        validFromMs: now - 1000,
        validToMs: now + 86400_000,
        sourceType: 'merchant_upload',
        sourceAssetId: 'flyer-123',
        sourceBox: [10, 20, 250, 300],
        verificationMethod: 'merchant_review'
      },
      now
    );
    expect(validResult).toEqual({ valid: true, errors: [] });

    const invalidResult = validateOfferForPublish(
      {
        retailerId: '',
        nameEn: 'A',
        amountMinor: 0,
        comparisonPriceMinor: 500,
        baseQuantity: null,
        baseUnit: null,
        buyUnits: 2,
        freeUnits: null,
        validFromMs: now - 10_000,
        validToMs: now - 1_000,
        sourceType: 'demo',
        sourceAssetId: '',
        sourceBox: [100, 100, 400, 400],
        verificationMethod: null
      },
      now
    );
    expect(invalidResult.valid).toBe(false);
    expect(invalidResult.errors).toEqual(
      expect.arrayContaining([
        'MISSING_RETAILER_ID',
        'INVALID_PRODUCT_NAME',
        'INVALID_PRICE_MINOR',
        'UNIT_OR_SIZE_UNSPECIFIED',
        'OFFER_ALREADY_EXPIRED',
        'DEMO_SOURCE_FORBIDDEN_IN_PUBLISH',
        'MISSING_SOURCE_ASSET',
        'INVALID_OR_GUESSED_SOURCE_BOX',
        'MISSING_VERIFICATION_RECEIPT',
        'INCOMPLETE_BOGO_TERMS'
      ])
    );
  });

  it('produces idempotent V2 backfill classifying unknown origin as UNVERIFIED and isolates demo records', () => {
    const v1Doc = {
      id: 'prod-1',
      retailerId: 'ret-1',
      name: 'Sunwhite Rice 5kg',
      brand: 'Sunwhite',
      currentPrice: 28.5,
      oldPrice: 35.0,
      unitSize: 5,
      unitType: 'kg',
      validFrom: 1_700_000_000_000,
      validTo: 1_700_500_000_000,
      status: 'published'
    };
    const patch1 = mapV1ProductToV2Patch('prod-1', v1Doc, 'run-1');
    const patch2 = mapV1ProductToV2Patch('prod-1', v1Doc, 'run-1');
    expect(patch1).toEqual(patch2);
    expect(patch1.rightsStatus).toBe('UNVERIFIED');
    expect(patch1.verificationLabel).toBe('unverified');
    expect(patch1.amountMinor).toBe(2850);
    expect(patch1.comparisonPriceMinor).toBe(3500);
    expect(patch1.baseQuantity).toBe(5000);
    expect(patch1.baseUnit).toBe('g');

    const dryRun = planV2ProductMigration([{ id: 'prod-1', data: v1Doc }], 'run-1');
    expect(dryRun.unverifiedCount).toBe(1);
    expect(dryRun.patchesPlanned).toBe(1);

    const demoCheck = checkDemoIsolation({
      products: [{ id: 'demo-1', isDemo: true, sourceType: 'demo' }],
      analytics: [{ id: 'evt-1', environment: 'demo' }]
    });
    expect(demoCheck.passed).toBe(false);
    expect(demoCheck.violations.length).toBe(2);

    const audit = auditDataset({
      branches: [{ id: 'b1', latitude: 0, longitude: 0, coordsVerified: false }],
      products: [v1Doc]
    }, 'snapshot', 1_700_600_000_000);
    expect(audit.issues.invalidOrUnverifiedBranches).toBe(1);
    expect(audit.issues.expiredPublishedProducts).toBe(1);
    expect(audit.issues.missingProvenanceOrRights).toBe(1);

    expect(buildDedupeKey('ret-1', { brand: 'Sunwhite', nameEn: 'Sunwhite Rice 5kg', baseQuantity: 5000, baseUnit: 'g', packCount: 1 }))
      .toContain('ret-1::norm::');
  });
});
