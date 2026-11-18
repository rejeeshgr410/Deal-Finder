import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { getNamedDb } from './config';
import { hashActorAnonymousId } from './analytics';
import {
  AssistantQueryInputSchema,
  AssistantOfferRecord,
  AssistantRetailerRecord,
  runGroundedAssistantQuery,
} from './assistantTools';

const MAX_ASSISTANT_REQUESTS_PER_HOUR = 30;
const MAX_CATALOG_DOCS = 500;

export function isAssistantFeatureEnabled(): boolean {
  return process.env.ENABLE_AI_ASSISTANT === 'true';
}

/**
 * Phase P1-A Grounded Deal Finder AI Assistant endpoint.
 * - Feature-flagged off by default (`ENABLE_AI_ASSISTANT=true` required to enable).
 * - Requires Firebase Authentication + hourly per-user rate limit.
 * - All pricing, unit comparisons, and basket totals are computed strictly by
 *   deterministic server-side tools against authorized, non-expired, non-demo offers.
 */
export const assistantQuery = onCall({ cors: true, timeoutSeconds: 30 }, async (request) => {
  if (!isAssistantFeatureEnabled()) {
    throw new HttpsError(
      'failed-precondition',
      'AI Shopping Assistant is currently disabled by feature flag.'
    );
  }

  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required to use the assistant.');
  }

  const parsed = AssistantQueryInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid assistant query payload.');
  }

  const db = getNamedDb();
  const now = Date.now();
  const oneHourAgo = now - 60 * 60 * 1000;
  const anonActor = hashActorAnonymousId(request.auth.uid);

  // Per-user hourly rate limit via PII-free analytics log
  const recentSnap = await db
    .collection('analytics')
    .where('eventName', '==', 'assistant_query')
    .where('actorAnonymousId', '==', anonActor)
    .where('occurredAt', '>=', oneHourAgo)
    .get();

  if (recentSnap.size >= MAX_ASSISTANT_REQUESTS_PER_HOUR) {
    throw new HttpsError('resource-exhausted', 'Hourly assistant query limit reached.');
  }

  // Bounded server-side catalog read (published only)
  const [retailersSnap, productsSnap] = await Promise.all([
    db.collection('retailers').where('status', '==', 'published').limit(200).get(),
    db.collection('products').where('status', '==', 'published').limit(MAX_CATALOG_DOCS).get(),
  ]);

  const retailers: AssistantRetailerRecord[] = retailersSnap.docs.map((doc) => {
    const d = doc.data();
    return {
      id: doc.id,
      name: String(d.name || ''),
      emirates: String(d.emirates || ''),
      status: String(d.status || ''),
    };
  });

  const catalog: AssistantOfferRecord[] = productsSnap.docs.map((doc) => {
    const d = doc.data();
    return {
      id: doc.id,
      retailerId: String(d.retailerId || ''),
      name: String(d.name || ''),
      brand: typeof d.brand === 'string' ? d.brand : '',
      category: typeof d.category === 'string' ? d.category : '',
      barcode: typeof d.barcode === 'string' ? d.barcode : '',
      masterProductId: typeof d.masterProductId === 'string' ? d.masterProductId : '',
      currentPrice: Number(d.currentPrice || 0),
      amountMinor: typeof d.amountMinor === 'number' ? d.amountMinor : undefined,
      unitSize: typeof d.unitSize === 'number' ? d.unitSize : undefined,
      unitType: typeof d.unitType === 'string' ? d.unitType : undefined,
      packCount: typeof d.packCount === 'number' ? d.packCount : 1,
      validFrom: typeof d.validFrom === 'number' ? d.validFrom : 0,
      validTo: typeof d.validTo === 'number' ? d.validTo : 0,
      status: String(d.status || ''),
      availability: typeof d.availability === 'string' ? d.availability : 'unknown',
      loyaltyRequired: Boolean(d.loyaltyRequired),
      promoConditions: typeof d.promoConditions === 'string' ? d.promoConditions : '',
      sourceType: typeof d.sourceType === 'string' ? d.sourceType : '',
      sourceLabel: typeof d.sourceLabel === 'string' ? d.sourceLabel : '',
      verificationLabel: typeof d.verificationLabel === 'string' ? d.verificationLabel : 'unverified',
      lastVerifiedAt: typeof d.lastVerifiedAt === 'number' ? d.lastVerifiedAt : undefined,
      isDemo: Boolean(d.isDemo),
    };
  });

  const response = runGroundedAssistantQuery(catalog, retailers, parsed.data, now);

  // PII-free usage & cost telemetry
  await db.collection('analytics').add({
    eventName: 'assistant_query',
    type: 'assistant_query',
    eventVersion: 2,
    occurredAt: now,
    timestamp: now,
    environment: process.env.FUNCTIONS_EMULATOR === 'true' ? 'emulator' : 'staging',
    source: 'backend_pipeline',
    actorAnonymousId: anonActor,
    sourceVerification: 'system',
    details: {
      intent: response.intent,
      emirate: response.emirate ?? 'unknown',
      language: response.language,
      matchCount: response.matches.length,
      injectionBlocked: response.injectionBlocked,
      llmTokensUsed: 0,
    },
  });

  return response;
});
