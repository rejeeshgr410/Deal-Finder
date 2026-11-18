import { getMessaging } from 'firebase-admin/messaging';
import { FieldValue } from 'firebase-admin/firestore';
import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { z } from 'zod';
import { getNamedDb } from './config';

export const NOTIFICATION_EVENT_STATUSES = [
  'created',
  'queued',
  'accepted_by_provider',
  'provider_rejected',
  'failed',
  'client_received',
  'opened',
] as const;

export type NotificationEventStatus = (typeof NOTIFICATION_EVENT_STATUSES)[number];

export const ALERT_TYPES = [
  'general',
  'flyer_launch',
  'promotion',
  'coupon',
  'retailer_update',
] as const;

export type AlertType = (typeof ALERT_TYPES)[number];

export const SendCampaignInputSchema = z.object({
  title: z.string().trim().min(1).max(60),
  message: z.string().trim().min(1).max(150),
  targetType: z.enum(['broadcast', 'emirate', 'retailer_followers']),
  targetEmirate: z.string().trim().min(1).max(64).nullable().optional(),
  targetRetailerId: z.string().trim().min(1).max(128).nullable().optional(),
  alertType: z.enum(ALERT_TYPES).default('general'),
  route: z.string().trim().min(1).max(120).optional().default('home'),
});

export const AckNotificationInputSchema = z.object({
  eventId: z.string().trim().min(1).max(128),
  status: z.enum(['client_received', 'opened']),
});

export const UnregisterTokenInputSchema = z.object({
  token: z.string().trim().min(10).max(512),
});

export interface UserNotificationPrefs {
  notifyNewFlyers?: boolean;
  notifyPriceDrops?: boolean;
  notifyDealExpiry?: boolean;
  notifyPromotions?: boolean;
  notifyCouponExpiry?: boolean;
  marketingOptIn?: boolean;
  notificationsPaused?: boolean;
}

/**
 * S6.1 preference & consent check:
 * - Respects per-category unsubscribe toggles.
 * - Marketing / broadcast promotions require opt-in (`marketingOptIn !== false` when explicitly set,
 *   and `notificationsPaused !== true`).
 */
export function isUserEligibleForAlert(
  prefs: UserNotificationPrefs | null | undefined,
  alertType: AlertType
): boolean {
  if (!prefs) {
    // Without explicit opt-in record, allow non-promotional updates only if not marketing broadcast
    return alertType !== 'promotion';
  }
  if (prefs.notificationsPaused === true) return false;

  switch (alertType) {
    case 'flyer_launch':
    case 'retailer_update':
      return prefs.notifyNewFlyers !== false;
    case 'coupon':
      return prefs.notifyCouponExpiry !== false;
    case 'promotion':
      return prefs.notifyPromotions !== false && prefs.marketingOptIn !== false;
    case 'general':
    default:
      return prefs.marketingOptIn !== false;
  }
}

export function isInvalidTokenError(errorCode: string | undefined): boolean {
  if (!errorCode) return false;
  return (
    errorCode === 'messaging/registration-token-not-registered' ||
    errorCode === 'messaging/invalid-registration-token'
  );
}

const MAX_CAMPAIGNS_PER_HOUR = 20;

export const sendNotificationCampaign = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const role = request.auth.token?.role as string | undefined;
  const callerRetailerId = request.auth.token?.retailerId as string | undefined;
  const isAdmin = role === 'admin';
  const isRetailer = role === 'retailer' && Boolean(callerRetailerId);

  if (!isAdmin && !isRetailer) {
    throw new HttpsError('permission-denied', 'Admin or retailer role required to dispatch notifications.');
  }

  const parsed = SendCampaignInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid notification campaign payload.');
  }
  const input = parsed.data;

  // Retailers may only target their own followers
  if (isRetailer && !isAdmin) {
    if (input.targetType !== 'retailer_followers' || input.targetRetailerId !== callerRetailerId) {
      throw new HttpsError('permission-denied', 'Retailers may only notify followers of their own store.');
    }
  }
  if (input.targetType === 'emirate' && !input.targetEmirate) {
    throw new HttpsError('invalid-argument', 'targetEmirate is required for emirate targeting.');
  }
  if (input.targetType === 'retailer_followers' && !input.targetRetailerId) {
    throw new HttpsError('invalid-argument', 'targetRetailerId is required for follower targeting.');
  }

  const db = getNamedDb();
  const now = Date.now();
  const oneHourAgo = now - 60 * 60 * 1000;

  // Basic per-actor hourly rate limit
  const recentSnap = await db
    .collection('notifications')
    .where('createdByUid', '==', request.auth.uid)
    .where('timestamp', '>=', oneHourAgo)
    .get();
  if (recentSnap.size >= MAX_CAMPAIGNS_PER_HOUR) {
    throw new HttpsError('resource-exhausted', 'Hourly notification rate limit exceeded.');
  }

  // 1. Create campaign record in `queued` state (never claims `delivered`)
  const campaignRef = await db.collection('notifications').add({
    title: input.title,
    message: input.message,
    targetType: input.targetType,
    targetEmirate: input.targetType === 'emirate' ? input.targetEmirate : null,
    targetRetailerId: input.targetType === 'retailer_followers' ? input.targetRetailerId : null,
    alertType: input.alertType,
    route: input.route,
    createdByUid: request.auth.uid,
    status: 'queued',
    matchedTokens: 0,
    optedOutSkipped: 0,
    acceptedByProvider: 0,
    providerRejected: 0,
    clientReceived: 0,
    opened: 0,
    timestamp: now,
  });

  // 2. Resolve active tokens matching audience + user opt-in preferences
  const tokensSnap = await db.collection('fcmTokens').where('status', '==', 'active').get();
  const eligibleTokens: Array<{ token: string; userId: string }> = [];
  let optedOutSkipped = 0;

  const userPrefsCache = new Map<string, UserNotificationPrefs | null>();

  for (const tokDoc of tokensSnap.docs) {
    const tokData = tokDoc.data();
    const token = tokDoc.id;
    const userId = typeof tokData.userId === 'string' ? tokData.userId : '';
    if (!userId) continue;

    let audienceMatch = false;
    if (input.targetType === 'broadcast') {
      audienceMatch = true;
    } else if (input.targetType === 'emirate') {
      audienceMatch = tokData.emirate === input.targetEmirate;
    } else if (input.targetType === 'retailer_followers') {
      audienceMatch = Array.isArray(tokData.followedRetailers) && tokData.followedRetailers.includes(input.targetRetailerId);
    }
    if (!audienceMatch) continue;

    if (!userPrefsCache.has(userId)) {
      const userSnap = await db.collection('users').doc(userId).get();
      userPrefsCache.set(userId, userSnap.exists ? (userSnap.data() as UserNotificationPrefs) : null);
    }
    const prefs = userPrefsCache.get(userId);
    if (!isUserEligibleForAlert(prefs, input.alertType)) {
      optedOutSkipped += 1;
      continue;
    }

    eligibleTokens.push({ token, userId });
  }

  let acceptedByProvider = 0;
  let providerRejected = 0;
  const isEmulator = process.env.FUNCTIONS_EMULATOR === 'true';

  // 3. Dispatch in chunks of 500 via Firebase Admin Messaging SDK
  const CHUNK_SIZE = 500;
  for (let i = 0; i < eligibleTokens.length; i += CHUNK_SIZE) {
    const chunk = eligibleTokens.slice(i, i + CHUNK_SIZE);
    const eventRefs = chunk.map(() => db.collection('notificationEvents').doc());

    // Pre-write queued event docs without raw shopper email/PII
    const batch = db.batch();
    chunk.forEach((entry, idx) => {
      batch.set(eventRefs[idx], {
        notificationId: campaignRef.id,
        userId: entry.userId,
        status: 'queued' as NotificationEventStatus,
        alertType: input.alertType,
        createdAt: now,
        updatedAt: now,
        providerMessageId: null,
        errorCode: null,
      });
    });
    await batch.commit();

    if (isEmulator) {
      // Do not hit real FCM endpoints from local emulator
      const emuBatch = db.batch();
      eventRefs.forEach((ref) => {
        acceptedByProvider += 1;
        emuBatch.update(ref, {
          status: 'accepted_by_provider' as NotificationEventStatus,
          providerMessageId: `emulator-${ref.id}`,
          updatedAt: Date.now(),
        });
      });
      await emuBatch.commit();
      continue;
    }

    const messaging = getMessaging();
    const response = await messaging.sendEachForMulticast({
      tokens: chunk.map((c) => c.token),
      notification: {
        title: input.title,
        body: input.message,
      },
      data: {
        notificationId: campaignRef.id,
        alertType: input.alertType,
        route: input.route,
      },
    });

    const resultBatch = db.batch();
    response.responses.forEach((res, idx) => {
      const eventRef = eventRefs[idx];
      const entry = chunk[idx];
      if (res.success) {
        acceptedByProvider += 1;
        resultBatch.update(eventRef, {
          status: 'accepted_by_provider' as NotificationEventStatus,
          providerMessageId: res.messageId ?? null,
          updatedAt: Date.now(),
        });
      } else {
        providerRejected += 1;
        const code = res.error?.code ?? 'unknown';
        resultBatch.update(eventRef, {
          status: 'provider_rejected' as NotificationEventStatus,
          errorCode: code,
          updatedAt: Date.now(),
        });
        if (isInvalidTokenError(code)) {
          resultBatch.update(db.collection('fcmTokens').doc(entry.token), {
            status: 'invalid',
            invalidatedAt: Date.now(),
          });
        }
      }
    });
    await resultBatch.commit();
  }

  const finalStatus =
    eligibleTokens.length === 0
      ? 'completed_no_audience'
      : acceptedByProvider > 0
      ? 'dispatched_to_provider'
      : 'provider_rejected';

  await campaignRef.update({
    status: finalStatus,
    matchedTokens: eligibleTokens.length,
    optedOutSkipped,
    acceptedByProvider,
    providerRejected,
    dispatchedAt: Date.now(),
  });

  return {
    notificationId: campaignRef.id,
    status: finalStatus,
    matchedTokens: eligibleTokens.length,
    optedOutSkipped,
    acceptedByProvider,
    providerRejected,
    note: 'Provider acceptance means FCM accepted the send request; it is not proof of device delivery.',
  };
});

export const acknowledgeNotificationEvent = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const parsed = AckNotificationInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid acknowledgement payload.');
  }

  const db = getNamedDb();
  const eventRef = db.collection('notificationEvents').doc(parsed.data.eventId);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(eventRef);
    if (!snap.exists) {
      throw new HttpsError('not-found', 'Notification event not found.');
    }
    const data = snap.data()!;
    if (data.userId !== request.auth!.uid) {
      throw new HttpsError('permission-denied', 'Cannot acknowledge another user event.');
    }

    const nextStatus = parsed.data.status;
    if (data.status === nextStatus || (data.status === 'opened' && nextStatus === 'client_received')) {
      return;
    }

    tx.update(eventRef, {
      status: nextStatus,
      updatedAt: Date.now(),
      ...(nextStatus === 'client_received' ? { clientReceivedAt: Date.now() } : { openedAt: Date.now() }),
    });

    if (typeof data.notificationId === 'string' && data.notificationId) {
      const field = nextStatus === 'client_received' ? 'clientReceived' : 'opened';
      tx.update(db.collection('notifications').doc(data.notificationId), {
        [field]: FieldValue.increment(1),
      });
    }
  });

  return { ok: true };
});

export const unregisterFcmToken = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const parsed = UnregisterTokenInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid token.');
  }

  const db = getNamedDb();
  const ref = db.collection('fcmTokens').doc(parsed.data.token);
  const snap = await ref.get();
  if (!snap.exists) return { ok: true };
  if (snap.data()?.userId !== request.auth.uid) {
    throw new HttpsError('permission-denied', 'Cannot revoke another user token.');
  }
  await ref.delete();
  return { ok: true };
});
