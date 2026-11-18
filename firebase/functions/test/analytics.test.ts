import { describe, it, expect } from 'vitest';
import {
  TrackAnalyticsInputSchema,
  sanitizeAnalyticsDetails,
  hashActorAnonymousId,
} from '../src/analytics';

describe('S6.2 Analytics schema & PII minimization', () => {
  it('strips PII keys from event details', () => {
    const cleaned = sanitizeAnalyticsDetails({
      query: 'basmati rice',
      email: 'shopper@example.com',
      phone: '+971500000000',
      fcmToken: 'secret-token',
      retailerId: 'ret-1',
    });
    expect(cleaned).toEqual({
      query: 'basmati rice',
      retailerId: 'ret-1',
    });
  });

  it('produces deterministic non-reversible anonymous actor IDs', () => {
    const a = hashActorAnonymousId('uid-123');
    const b = hashActorAnonymousId('uid-123');
    const c = hashActorAnonymousId('uid-456');
    expect(a).toBe(b);
    expect(a).not.toBe(c);
    expect(a).not.toContain('uid-123');
  });

  it('accepts S6.2 event names and rejects unknown event names', () => {
    expect(
      TrackAnalyticsInputSchema.safeParse({
        eventName: 'basket_comparison_completed',
        source: 'android_app',
        tenantId: 'ret-1',
        sourceVerification: 'verified',
      }).success
    ).toBe(true);

    expect(
      TrackAnalyticsInputSchema.safeParse({
        eventName: 'manual_correction',
        source: 'merchant_portal',
      }).success
    ).toBe(true);

    expect(
      TrackAnalyticsInputSchema.safeParse({
        eventName: 'fake_revenue_attribution',
      }).success
    ).toBe(false);
  });
});
