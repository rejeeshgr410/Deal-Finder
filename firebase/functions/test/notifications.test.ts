import { describe, it, expect } from 'vitest';
import {
  isUserEligibleForAlert,
  isInvalidTokenError,
  SendCampaignInputSchema,
  AckNotificationInputSchema,
  NOTIFICATION_EVENT_STATUSES,
} from '../src/notifications';

describe('S6.1 Notification truth model & preference enforcement', () => {
  it('never includes fake "delivered" as an automatic provider status', () => {
    expect(NOTIFICATION_EVENT_STATUSES).toContain('accepted_by_provider');
    expect(NOTIFICATION_EVENT_STATUSES).toContain('provider_rejected');
    expect(NOTIFICATION_EVENT_STATUSES).toContain('client_received');
    expect(NOTIFICATION_EVENT_STATUSES).toContain('opened');
    expect(NOTIFICATION_EVENT_STATUSES).not.toContain('delivered');
  });

  it('respects per-category opt-out and global pause', () => {
    expect(isUserEligibleForAlert({ notificationsPaused: true, notifyNewFlyers: true }, 'flyer_launch')).toBe(false);
    expect(isUserEligibleForAlert({ notifyNewFlyers: false }, 'flyer_launch')).toBe(false);
    expect(isUserEligibleForAlert({ notifyNewFlyers: true }, 'flyer_launch')).toBe(true);
    expect(isUserEligibleForAlert({ notifyPromotions: true, marketingOptIn: false }, 'promotion')).toBe(false);
    expect(isUserEligibleForAlert({ notifyPromotions: true, marketingOptIn: true }, 'promotion')).toBe(true);
    expect(isUserEligibleForAlert({ notifyCouponExpiry: false }, 'coupon')).toBe(false);
    expect(isUserEligibleForAlert(null, 'promotion')).toBe(false);
  });

  it('identifies unregistered FCM tokens for cleanup', () => {
    expect(isInvalidTokenError('messaging/registration-token-not-registered')).toBe(true);
    expect(isInvalidTokenError('messaging/invalid-registration-token')).toBe(true);
    expect(isInvalidTokenError('messaging/internal-error')).toBe(false);
    expect(isInvalidTokenError(undefined)).toBe(false);
  });

  it('validates campaign and acknowledgement payloads strictly', () => {
    expect(
      SendCampaignInputSchema.safeParse({
        title: 'Weekend Deals',
        message: 'Fresh offers in Dubai',
        targetType: 'emirate',
        targetEmirate: 'Dubai',
        alertType: 'flyer_launch',
      }).success
    ).toBe(true);

    expect(
      SendCampaignInputSchema.safeParse({
        title: '',
        message: 'Missing title',
        targetType: 'broadcast',
      }).success
    ).toBe(false);

    expect(
      AckNotificationInputSchema.safeParse({ eventId: 'evt-1', status: 'client_received' }).success
    ).toBe(true);
    expect(
      AckNotificationInputSchema.safeParse({ eventId: 'evt-1', status: 'delivered' }).success
    ).toBe(false);
  });
});
