import * as crypto from 'crypto';
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
  'basket_comparison_completed',
  'merchant_offer_published',
  'ai_extraction_completed',
  'manual_correction',
  'pilot_test',
] as const;

export type AnalyticsEventName = (typeof ALLOWED_ANALYTICS_EVENTS)[number];

export const TrackAnalyticsInputSchema = z.object({
  eventName: z.enum(ALLOWED_ANALYTICS_EVENTS).optional(),
  type: z.enum(ALLOWED_ANALYTICS_EVENTS).optional(),
  eventVersion: z.number().int().min(1).max(10).optional().default(2),
  environment: z.enum(['staging', 'emulator', 'production']).optional(),
  source: z.enum(['android_app', 'merchant_portal', 'backend_pipeline']).optional().default('android_app'),
  tenantId: z.string().min(1).max(128).optional(),
  recordId: z.string().min(1).max(128).optional(),
  sourceVerification: z.enum(['verified', 'unverified', 'system']).optional().default('unverified'),
  requestId: z.string().min(1).max(128).optional(),
  details: z.record(z.union([z.string().max(200), z.number(), z.boolean(), z.null()])).optional().default({}),
}).refine((d) => Boolean(d.eventName || d.type), {
  message: 'Either eventName or type is required.',
});

const PII_KEYS = new Set([
  'email',
  'userEmail',
  'phone',
  'phoneNumber',
  'token',
  'fcmToken',
  'password',
  'address',
  'fullName',
]);

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

export function hashActorAnonymousId(uid: string): string {
  const salt = process.env.ANALYTICS_HASH_SALT || 'dealfinder-uae-anon-v2';
  return crypto.createHash('sha256').update(`${salt}:${uid}`).digest('hex').slice(0, 24);
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
  const eventName = (parsed.data.eventName ?? parsed.data.type)!;
  const now = Date.now();

  await db.collection('analytics').add({
    eventName,
    type: eventName, // backward-compatible alias for existing readers
    eventVersion: parsed.data.eventVersion,
    occurredAt: now,
    timestamp: now, // backward-compatible alias
    environment,
    source: parsed.data.source,
    actorAnonymousId: hashActorAnonymousId(request.auth.uid),
    tenantId: parsed.data.tenantId ?? null,
    recordId: parsed.data.recordId ?? null,
    sourceVerification: parsed.data.sourceVerification,
    requestId: parsed.data.requestId ?? crypto.randomUUID(),
    details: sanitizeAnalyticsDetails(parsed.data.details),
  });

  return { ok: true };
});
