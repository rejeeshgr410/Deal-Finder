import { describe, it, expect } from 'vitest';
import { canCallerAssignRole, requiresRetailerId } from '../auth';
import {
  clampBox2d,
  sanitizeExtractionItem,
  canAccessRetailerExtraction,
  GeminiExtractionOutputSchema,
  resolveGenAiClientOptions,
  DEFAULT_VERTEX_PROJECT,
  DEFAULT_VERTEX_LOCATION
} from '../gemini';
import { sanitizeAnalyticsDetails, TrackAnalyticsInputSchema } from '../analytics';

describe('Role assignment authorization helpers', () => {
  it('blocks non-admin callers from assigning any role', () => {
    expect(canCallerAssignRole('consumer', 'consumer', 'admin').allowed).toBe(false);
    expect(canCallerAssignRole('retailer_owner', 'pending', 'retailer_staff').allowed).toBe(false);
    expect(canCallerAssignRole('moderator', 'pending', 'data_entry').allowed).toBe(false);
  });

  it('prevents admin from granting super_admin or modifying super_admin', () => {
    expect(canCallerAssignRole('admin', 'pending', 'super_admin').allowed).toBe(false);
    expect(canCallerAssignRole('admin', 'super_admin', 'consumer').allowed).toBe(false);
    expect(canCallerAssignRole('admin', 'pending', 'retailer_owner').allowed).toBe(true);
    expect(canCallerAssignRole('super_admin', 'pending', 'super_admin').allowed).toBe(true);
  });

  it('requires retailerId only for retailer roles', () => {
    expect(requiresRetailerId('retailer_owner')).toBe(true);
    expect(requiresRetailerId('retailer_staff')).toBe(true);
    expect(requiresRetailerId('admin')).toBe(false);
    expect(requiresRetailerId('consumer')).toBe(false);
  });
});

describe('Gemini flyer extraction validation & sanitization', () => {
  it('clamps valid bounding boxes and rejects inverted/non-finite/guessed boxes', () => {
    expect(clampBox2d([-10, 50, 1200, 500])).toEqual({ box2d: [0, 50, 1000, 500], valid: true });
    expect(clampBox2d([500, 500, 100, 200])).toEqual({ box2d: null, valid: false });
    expect(clampBox2d([100, 100, 400, 400])).toEqual({ box2d: null, valid: false });
  });

  it('flags zero price, inverted comparator price, missing unit, and promo conditions without fake confidence', () => {
    const parsed = GeminiExtractionOutputSchema.parse({
      products: [
        {
          name: 'Almarai Fresh Milk',
          brand: 'Almarai',
          category: 'Dairy & Eggs',
          currentPrice: 0,
          oldPrice: 12,
          unitSize: '',
          unitType: '',
          promoConditions: 'Buy 2 get 1 free',
          box2d: [10, 20, 200, 300]
        }
      ]
    });

    const sanitized = sanitizeExtractionItem(parsed.products[0], 1);
    expect(sanitized.reviewReasons).toContain('PRICE_ZERO_OR_MISSING');
    expect(sanitized.reviewReasons).toContain('UNIT_OR_PACK_UNSPECIFIED');
    expect(sanitized.reviewReasons).toContain('PROMO_CONDITIONS_PRESENT');
    expect((sanitized as any).confidence).toBeUndefined();
  });

  it('enforces tenant isolation on extraction calls', () => {
    expect(canAccessRetailerExtraction('retailer_owner', 'retailer-a', 'retailer-a')).toBe(true);
    expect(canAccessRetailerExtraction('retailer_owner', 'retailer-a', 'retailer-b')).toBe(false);
    expect(canAccessRetailerExtraction('consumer', null, 'retailer-a')).toBe(false);
    expect(canAccessRetailerExtraction('moderator', null, 'retailer-b')).toBe(true);
  });

  it('defaults to Vertex AI options on gen-lang-client-0615495350 and falls back to API key only when disabled', () => {
    expect(
      resolveGenAiClientOptions(
        { GCLOUD_PROJECT: 'deal-finder-uae', GOOGLE_CLOUD_LOCATION: 'us-central1' } as unknown as NodeJS.ProcessEnv,
        'unused-secret'
      )
    ).toEqual({
      vertexai: true,
      project: 'gen-lang-client-0615495350',
      location: DEFAULT_VERTEX_LOCATION
    });

    expect(resolveGenAiClientOptions({} as NodeJS.ProcessEnv, 'unused-secret')).toEqual({
      vertexai: true,
      project: DEFAULT_VERTEX_PROJECT,
      location: DEFAULT_VERTEX_LOCATION
    });

    expect(
      resolveGenAiClientOptions(
        {
          VERTEX_AI_PROJECT: 'custom-proj',
          VERTEX_AI_LOCATION: 'global'
        } as unknown as NodeJS.ProcessEnv,
        ''
      )
    ).toEqual({
      vertexai: true,
      project: 'custom-proj',
      location: 'global'
    });

    expect(
      resolveGenAiClientOptions(
        { USE_VERTEX_AI: 'false' } as unknown as NodeJS.ProcessEnv,
        'secret-key-123'
      )
    ).toEqual({ apiKey: 'secret-key-123' });

    expect(() =>
      resolveGenAiClientOptions({ USE_VERTEX_AI: 'false' } as unknown as NodeJS.ProcessEnv, '')
    ).toThrow();
  });
});

describe('Analytics sanitization', () => {
  it('strips PII keys while retaining safe telemetry fields', () => {
    const parsed = TrackAnalyticsInputSchema.parse({
      type: 'product_click',
      tenantId: 'retailer-a',
      details: {
        productId: 'p-1',
        userEmail: 'leak@example.com',
        email: 'leak@example.com',
        fcmToken: 'secret-token'
      }
    });
    const clean = sanitizeAnalyticsDetails(parsed.details);
    expect(clean).toEqual({ productId: 'p-1' });
  });
});
