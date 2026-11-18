import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { z } from 'zod';
import { getNamedDb } from './config';

export const ALLOWED_ANALYTICS_EVENTS = [
  'flyer_view',
  'product_click',
  'search',
  'coupon_save',
  'retailer_page_view',
  'promotion_click',
  'ocr_metrics',
  'basket_comparison_completed'
] as const;

export const TrackAnalyticsInputSchema = z.object({
  type: z.enum(ALLOWED_ANALYTICS_EVENTS),
  tenantId: z.string().min(1).max(128).optional(),
  environment: z.enum(['staging', 'emulator', 'production']).optional(),
  details: z.record(z.union([z.string().max(200), z.number(), z.boolean(), z.null()])).optional().default({})
});

const PII_KEYS = new Set(['email', 'userEmail', 'phone', 'token', 'fcmToken', 'password']);

export function sanitizeAnalyticsDetails(
  details: Record<string, string | number | boolean | null>
): Record<string, string | number | boolean | null> {
  const sanitized: Record<string, string | number | boolean | null> = {};
  for (const [key, value] of Object.entries(details)) {
    if (!PII_KEYS.has(key)) {
      sanitized[key] = value;
    }
  }
  return sanitized;
}

export const trackAnalyticsEvent = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required to record analytics.');
  }

  const parsed = TrackAnalyticsInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid analytics payload.');
  }

  const db = getNamedDb();
  const isEmulator = process.env.FUNCTIONS_EMULATOR === 'true';
  const environment = isEmulator ? 'emulator' : (parsed.data.environment ?? 'staging');
  const now = Date.now();

  await db.collection('analytics').add({
    type: parsed.data.type,
    eventVersion: 2,
    actorUid: request.auth.uid,
    tenantId: parsed.data.tenantId ?? null,
    environment,
    details: sanitizeAnalyticsDetails(parsed.data.details),
    timestamp: now
  });

  return { ok: true };
});
