import { describe, it, expect } from 'vitest';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  buildDeterministicProductId,
  isPublisherRole,
  isTenantStaffOrModerator,
  isValidJobTransition,
  MAX_ATOMIC_PUBLISH_PRODUCTS,
  PublishFlyerInputSchema,
  validateBatchForAtomicPublish
} from '../flyerPipeline';
import { runExtractionBenchmark } from '../../../../scripts/benchmark-extraction';

describe('Flyer Extraction Job State Machine & Authorization', () => {
  it('allows valid state transitions and rejects invalid jumps', () => {
    expect(isValidJobTransition('uploaded', 'queued')).toBe(true);
    expect(isValidJobTransition('queued', 'extracting')).toBe(true);
    expect(isValidJobTransition('extracting', 'review_required')).toBe(true);
    expect(isValidJobTransition('extracting', 'failed_retryable')).toBe(true);
    expect(isValidJobTransition('failed_retryable', 'extracting')).toBe(true);
    expect(isValidJobTransition('review_required', 'approved')).toBe(true);
    expect(isValidJobTransition('approved', 'publishing')).toBe(true);
    expect(isValidJobTransition('publishing', 'published')).toBe(true);

    // Invalid shortcuts or post-terminal transitions
    expect(isValidJobTransition('uploaded', 'published')).toBe(false);
    expect(isValidJobTransition('failed_final', 'queued')).toBe(false);
    expect(isValidJobTransition('cancelled', 'extracting')).toBe(false);
  });

  it('restricts global publishing to moderator/admin/super_admin', () => {
    expect(isPublisherRole('super_admin')).toBe(true);
    expect(isPublisherRole('admin')).toBe(true);
    expect(isPublisherRole('moderator')).toBe(true);
    expect(isPublisherRole('retailer_owner')).toBe(false);
    expect(isPublisherRole('consumer')).toBe(false);

    expect(isTenantStaffOrModerator('retailer_owner', 'ret-a', 'ret-a')).toBe(true);
    expect(isTenantStaffOrModerator('retailer_owner', 'ret-a', 'ret-b')).toBe(false);
  });
});

describe('Atomic Idempotent Publication Pre-Checks', () => {
  const now = 1_700_000_000_000;

  function makeValidItem(index: number) {
    return {
      itemId: `item-${index}`,
      name: `Almarai Fresh Milk ${index}`,
      brand: 'Almarai',
      category: 'Dairy & Eggs',
      barcode: '',
      masterProductId: '',
      currentPrice: 10.5,
      oldPrice: 12.5,
      unitSize: '2',
      unitType: 'L',
      packCount: 1,
      minUnits: 1,
      buyUnits: null,
      freeUnits: null,
      loyaltyRequired: false,
      promoConditions: '',
      imageUrl: 'data:image/jpeg;base64,abc',
      sourcePage: 1,
      sourceBox: [10, 20, 200, 300] as [number, number, number, number],
      reviewReasons: [],
      acknowledged: false,
      editedFields: []
    };
  }

  it('derives deterministic revision-scoped product IDs and passes valid batches', () => {
    const id1 = buildDeterministicProductId('flyer-123', 2, 'gemini-1-0');
    const id2 = buildDeterministicProductId('flyer-123', 2, 'gemini-1-0');
    expect(id1).toBe(id2);
    expect(id1).toBe('flyer-123_r2_gemini-1-0');

    const input = PublishFlyerInputSchema.parse({
      flyerId: 'flyer-123',
      retailerId: 'ret-a',
      idempotencyKey: 'flyer-123_rev2_1',
      publicationRevision: 2,
      validFromMs: now - 1000,
      validToMs: now + 86_400_000,
      sourceType: 'merchant_upload',
      sourceAssetId: 'private_extractions/ret-a/test.pdf',
      items: [makeValidItem(1)]
    });

    const result = validateBatchForAtomicPublish(input, now);
    expect(result.valid).toBe(true);
    expect(result.normalizedItems).toHaveLength(1);
    expect(result.normalizedItems[0].id).toBe('flyer-123_r2_item-1');
    expect(result.normalizedItems[0].amountMinor).toBe(1050);
    expect(result.normalizedItems[0].baseQuantity).toBe(2000);
    expect(result.normalizedItems[0].baseUnit).toBe('ml');
  });

  it('rejects batches exceeding MAX_ATOMIC_PUBLISH_PRODUCTS (300) before any write', () => {
    const items = Array.from({ length: MAX_ATOMIC_PUBLISH_PRODUCTS + 1 }, (_, i) => makeValidItem(i));
    const input = PublishFlyerInputSchema.parse({
      flyerId: 'flyer-big',
      retailerId: 'ret-a',
      idempotencyKey: 'flyer-big_rev1',
      publicationRevision: 1,
      validFromMs: now - 1000,
      validToMs: now + 86_400_000,
      sourceType: 'merchant_upload',
      sourceAssetId: 'private_extractions/ret-a/big.pdf',
      items
    });

    const result = validateBatchForAtomicPublish(input, now);
    expect(result.valid).toBe(false);
    expect(result.normalizedItems).toHaveLength(0);
    expect(result.errors[0].codes).toContain('EXCEEDS_ATOMIC_BATCH_BUDGET');
  });

  it('blocks unacknowledged flagged items, guessed boxes, and expired flyers', () => {
    const flaggedItem = {
      ...makeValidItem(1),
      reviewReasons: ['PROMO_CONDITIONS_PRESENT'],
      acknowledged: false,
      sourceBox: [100, 100, 400, 400] as [number, number, number, number]
    };
    const input = PublishFlyerInputSchema.parse({
      flyerId: 'flyer-bad',
      retailerId: 'ret-a',
      idempotencyKey: 'flyer-bad_rev1',
      publicationRevision: 1,
      validFromMs: now - 10_000,
      validToMs: now - 1000,
      sourceType: 'merchant_upload',
      sourceAssetId: 'private_extractions/ret-a/bad.pdf',
      items: [flaggedItem]
    });

    const result = validateBatchForAtomicPublish(input, now);
    expect(result.valid).toBe(false);
    expect(result.errors[0].codes).toEqual(
      expect.arrayContaining([
        'UNACKNOWLEDGED_REVIEW_FLAGS',
        'INVALID_OR_GUESSED_SOURCE_BOX',
        'OFFER_ALREADY_EXPIRED'
      ])
    );
  });
});

describe('30-row Extraction Benchmark Fixture', () => {
  it('evaluates all 30 fixture rows with explicit denominators and catches invalid zero-price/box rows', () => {
    const __dirname = path.dirname(fileURLToPath(import.meta.url));
    const fixturePath = path.resolve(__dirname, '../../../../test-fixtures/annotations/uae_flyer_benchmark_v1.json');
    const metrics = runExtractionBenchmark(fixturePath);

    expect(metrics.totalRows).toBe(30);
    expect(metrics.eligiblePublishedCheckRows).toBe(29);
    expect(metrics.productNameExactRate).toBe(1);
    expect(metrics.priceExactRate).toBe(1);
    expect(metrics.sizeAndPackExactRate).toBe(1);
    expect(metrics.promoConditionExactRate).toBe(1);
    expect(metrics.falseDuplicateRate).toBe(0);
    expect(metrics.flaggedInvalidRowsCaught).toBe(1);
  });
});
