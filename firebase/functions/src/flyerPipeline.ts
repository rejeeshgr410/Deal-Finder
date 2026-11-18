import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { z } from 'zod';
import { getNamedDb } from './config';
import {
  aedToFils,
  buildDedupeKey,
  isValidGtin,
  normalizeQuantityUnit,
  validateOfferForPublish
} from './contracts/offerV2';

export const MAX_ATOMIC_PUBLISH_PRODUCTS = 300;

export type ExtractionJobState =
  | 'uploaded'
  | 'queued'
  | 'extracting'
  | 'review_required'
  | 'approved'
  | 'publishing'
  | 'published'
  | 'failed_retryable'
  | 'failed_final'
  | 'cancelled'
  | 'rejected'
  | 'expired';

export const ALLOWED_JOB_TRANSITIONS: Record<ExtractionJobState, ReadonlyArray<ExtractionJobState>> = {
  uploaded: ['queued', 'cancelled', 'failed_final'],
  queued: ['extracting', 'cancelled', 'failed_retryable', 'failed_final'],
  extracting: ['review_required', 'failed_retryable', 'failed_final', 'cancelled'],
  review_required: ['extracting', 'approved', 'rejected', 'cancelled', 'expired'],
  approved: ['publishing', 'review_required', 'rejected', 'cancelled'],
  publishing: ['published', 'failed_retryable', 'failed_final'],
  published: ['expired', 'rejected'],
  failed_retryable: ['queued', 'extracting', 'failed_final', 'cancelled'],
  failed_final: [],
  cancelled: [],
  rejected: [],
  expired: []
};

export function isValidJobTransition(from: ExtractionJobState, to: ExtractionJobState): boolean {
  return ALLOWED_JOB_TRANSITIONS[from]?.includes(to) ?? false;
}

export function isPublisherRole(role: string): boolean {
  return role === 'super_admin' || role === 'admin' || role === 'moderator';
}

export function isTenantStaffOrModerator(
  role: string,
  callerRetailerId: string | null | undefined,
  targetRetailerId: string
): boolean {
  if (isPublisherRole(role) || role === 'data_entry') return true;
  return (role === 'retailer_owner' || role === 'retailer_staff') && callerRetailerId === targetRetailerId;
}

export const CreateJobInputSchema = z.object({
  flyerId: z.string().min(1).max(128),
  retailerId: z.string().min(1).max(128),
  sourceAssetId: z.string().min(1).max(512),
  totalPages: z.number().int().min(1).max(50)
});

export const PublishItemInputSchema = z.object({
  itemId: z.string().min(1).max(128),
  name: z.string().min(2).max(240),
  brand: z.string().max(120).optional().default(''),
  category: z.string().max(120).optional().default('General'),
  barcode: z.string().max(32).optional().default(''),
  masterProductId: z.string().max(128).optional().default(''),
  currentPrice: z.number().positive(),
  oldPrice: z.number().nonnegative().nullable().optional(),
  unitSize: z.union([z.string(), z.number()]),
  unitType: z.string().min(1).max(40),
  packCount: z.number().int().min(1).max(1000).optional().default(1),
  minUnits: z.number().int().min(1).max(100).optional().default(1),
  buyUnits: z.number().int().min(1).max(50).nullable().optional(),
  freeUnits: z.number().int().min(1).max(50).nullable().optional(),
  loyaltyRequired: z.boolean().optional().default(false),
  promoConditions: z.string().max(240).optional().default(''),
  imageUrl: z.string().max(2_000_000).optional().default(''),
  sourcePage: z.number().int().min(1).max(50),
  sourceBox: z.tuple([z.number(), z.number(), z.number(), z.number()]).nullable().optional(),
  reviewReasons: z.array(z.string().max(64)).optional().default([]),
  acknowledged: z.boolean().optional().default(false),
  editedFields: z.array(z.string().max(64)).optional().default([]),
  modelName: z.string().max(64).optional(),
  promptRevision: z.string().max(64).optional()
});

export const PublishFlyerInputSchema = z.object({
  flyerId: z.string().min(1).max(128),
  retailerId: z.string().min(1).max(128),
  jobId: z.string().min(1).max(128).optional(),
  idempotencyKey: z.string().min(8).max(128),
  publicationRevision: z.number().int().min(1).max(10_000),
  validFromMs: z.number().finite().positive(),
  validToMs: z.number().finite().positive(),
  sourceType: z.enum(['merchant_upload', 'official_feed', 'licensed_import']).default('merchant_upload'),
  sourceAssetId: z.string().min(1).max(512),
  items: z.array(PublishItemInputSchema).min(1).max(MAX_ATOMIC_PUBLISH_PRODUCTS + 1)
});

export function buildDeterministicProductId(
  flyerId: string,
  publicationRevision: number,
  itemId: string
): string {
  const safeFlyer = flyerId.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 48);
  const safeItem = itemId.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 64);
  return `${safeFlyer}_r${publicationRevision}_${safeItem}`;
}

export function validateBatchForAtomicPublish(
  input: z.infer<typeof PublishFlyerInputSchema>,
  nowMs: number = Date.now()
): { valid: boolean; errors: Array<{ itemId?: string; codes: string[] }>; normalizedItems: Array<Record<string, any>> } {
  const errors: Array<{ itemId?: string; codes: string[] }> = [];

  if (input.items.length > MAX_ATOMIC_PUBLISH_PRODUCTS) {
    errors.push({ codes: ['EXCEEDS_ATOMIC_BATCH_BUDGET'] });
    return { valid: false, errors, normalizedItems: [] };
  }

  const normalizedItems: Array<Record<string, any>> = [];

  for (const item of input.items) {
    const itemErrors: string[] = [];

    if ((item.reviewReasons?.length ?? 0) > 0 && !item.acknowledged) {
      itemErrors.push('UNACKNOWLEDGED_REVIEW_FLAGS');
    }

    const normUnit = normalizeQuantityUnit(item.unitSize, item.unitType, item.packCount);
    if (!normUnit) {
      itemErrors.push('UNIT_OR_SIZE_UNSPECIFIED');
    }

    const amountMinor = aedToFils(item.currentPrice);
    const oldNum = item.oldPrice ?? 0;
    const comparisonPriceMinor = oldNum > 0 ? aedToFils(oldNum) : null;
    const gtin = isValidGtin(item.barcode) ? item.barcode.trim() : undefined;

    const candidateCheck = validateOfferForPublish(
      {
        retailerId: input.retailerId,
        nameEn: item.name,
        brand: item.brand,
        barcodeGtin: gtin,
        amountMinor,
        comparisonPriceMinor,
        currency: 'AED',
        baseQuantity: normUnit?.baseQuantity ?? null,
        baseUnit: normUnit?.baseUnit ?? null,
        packCount: normUnit?.packCount ?? item.packCount,
        minUnits: item.minUnits,
        buyUnits: item.buyUnits ?? null,
        freeUnits: item.freeUnits ?? null,
        validFromMs: input.validFromMs,
        validToMs: input.validToMs,
        sourceType: input.sourceType,
        sourceAssetId: input.sourceAssetId,
        sourceBox: item.sourceBox ?? null,
        verificationMethod: 'admin_review',
        availability: 'unknown'
      },
      nowMs
    );

    if (!candidateCheck.valid) {
      itemErrors.push(...candidateCheck.errors);
    }

    if (itemErrors.length > 0) {
      errors.push({ itemId: item.itemId, codes: Array.from(new Set(itemErrors)) });
      continue;
    }

    const prodId = buildDeterministicProductId(input.flyerId, input.publicationRevision, item.itemId);
    const dedupeKey = buildDedupeKey(input.retailerId, {
      masterProductId: item.masterProductId || undefined,
      barcodeGtin: gtin,
      brand: item.brand,
      nameEn: item.name,
      baseQuantity: normUnit?.baseQuantity,
      baseUnit: normUnit?.baseUnit,
      packCount: normUnit?.packCount
    });

    const keywords = Array.from(
      new Set(
        `${item.name} ${item.brand} ${item.category}`
          .toLowerCase()
          .replace(/[^a-z0-9\s]/g, ' ')
          .split(/\s+/)
          .filter((w) => w.length >= 2)
      )
    );

    normalizedItems.push({
      id: prodId,
      itemId: item.itemId,
      flyerId: input.flyerId,
      retailerId: input.retailerId,
      masterProductId: item.masterProductId || '',
      name: item.name.trim(),
      normalizedName: item.name.trim().toLowerCase(),
      searchKeywords: keywords,
      brand: item.brand.trim() || 'Flyer Deal',
      category: item.category.trim() || 'General',
      barcode: gtin ?? '',
      barcodeGtin: gtin ?? null,
      imageUrl: item.imageUrl || '',
      currentPrice: Math.round(item.currentPrice * 100) / 100,
      oldPrice: comparisonPriceMinor ? comparisonPriceMinor / 100 : null,
      unitSize: typeof item.unitSize === 'number' ? item.unitSize : parseFloat(String(item.unitSize)) || 1,
      unitType: item.unitType.trim() || 'unit',
      validFrom: input.validFromMs,
      validTo: input.validToMs,
      validFromIso: new Date(input.validFromMs).toISOString(),
      validToIso: new Date(input.validToMs).toISOString(),
      status: 'published',
      schemaVersion: 2,
      currency: 'AED',
      amountMinor,
      comparisonPriceMinor,
      baseQuantity: normUnit!.baseQuantity,
      baseUnit: normUnit!.baseUnit,
      packCount: normUnit!.packCount,
      minUnits: item.minUnits ?? 1,
      buyUnits: item.buyUnits ?? null,
      freeUnits: item.freeUnits ?? null,
      loyaltyRequired: item.loyaltyRequired ?? false,
      promoConditions: item.promoConditions.trim(),
      availability: 'unknown',
      sourceType: input.sourceType,
      sourceLabel: 'Merchant Flyer (Verified)',
      sourceAssetId: input.sourceAssetId,
      sourcePage: item.sourcePage,
      sourceBox: item.sourceBox ?? null,
      verificationLabel: 'admin_reviewed',
      verificationMethod: 'admin_review',
      lastVerifiedAt: nowMs,
      editedFields: item.editedFields ?? [],
      modelName: item.modelName ?? null,
      promptRevision: item.promptRevision ?? null,
      publicationRevision: input.publicationRevision,
      idempotencyKey: input.idempotencyKey,
      dedupeKey,
      isDemo: false
    });
  }

  return { valid: errors.length === 0, errors, normalizedItems };
}

async function resolveCallerAuth(uid: string, tokenClaims: Record<string, any>) {
  const db = getNamedDb();
  const userSnap = await db.collection('users').doc(uid).get();
  const userData = userSnap.exists ? userSnap.data() : undefined;
  const isSuspended = userData?.isSuspended === true;
  const role =
    typeof tokenClaims.role === 'string' && tokenClaims.role.length > 0
      ? tokenClaims.role
      : (typeof userData?.role === 'string' ? userData.role : 'consumer');
  const retailerId =
    typeof tokenClaims.retailerId === 'string' && tokenClaims.retailerId.length > 0
      ? tokenClaims.retailerId
      : (typeof userData?.retailerId === 'string' ? userData.retailerId : null);
  return { role, retailerId, isSuspended };
}

export const createFlyerExtractionJob = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const parsed = CreateJobInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid job creation payload.');
  }

  const caller = await resolveCallerAuth(request.auth.uid, request.auth.token);
  if (caller.isSuspended) {
    throw new HttpsError('permission-denied', 'Suspended users cannot create extraction jobs.');
  }
  if (!isTenantStaffOrModerator(caller.role, caller.retailerId, parsed.data.retailerId)) {
    throw new HttpsError('permission-denied', 'Caller is not authorized for this retailer.');
  }

  const db = getNamedDb();
  const now = Date.now();
  const jobRef = db.collection('extractionJobs').doc();
  await jobRef.set({
    id: jobRef.id,
    flyerId: parsed.data.flyerId,
    retailerId: parsed.data.retailerId,
    sourceAssetId: parsed.data.sourceAssetId,
    totalPages: parsed.data.totalPages,
    state: 'queued' as ExtractionJobState,
    retryCount: 0,
    actorUid: request.auth.uid,
    history: [
      { state: 'uploaded', actorUid: request.auth.uid, timestamp: now },
      { state: 'queued', actorUid: request.auth.uid, timestamp: now }
    ],
    createdAt: now,
    updatedAt: now
  });

  return { jobId: jobRef.id, state: 'queued' };
});

export const publishFlyer = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }

  const parsed = PublishFlyerInputSchema.safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid publish flyer payload.');
  }

  const caller = await resolveCallerAuth(request.auth.uid, request.auth.token);
  if (caller.isSuspended || !isPublisherRole(caller.role)) {
    throw new HttpsError('permission-denied', 'Only authorized moderators or admins may publish flyers.');
  }

  const now = Date.now();
  const validation = validateBatchForAtomicPublish(parsed.data, now);
  if (!validation.valid) {
    const hasBudgetErr = validation.errors.some((e) => e.codes.includes('EXCEEDS_ATOMIC_BATCH_BUDGET'));
    throw new HttpsError(
      hasBudgetErr ? 'failed-precondition' : 'invalid-argument',
      `Publication validation failed: ${JSON.stringify(validation.errors.slice(0, 5))}`
    );
  }

  const db = getNamedDb();
  const flyerRef = db.collection('flyers').doc(parsed.data.flyerId);

  const txResult = await db.runTransaction(async (tx) => {
    const flyerSnap = await tx.get(flyerRef);
    if (!flyerSnap.exists) {
      throw new HttpsError('not-found', 'Flyer document not found.');
    }
    const flyerData = flyerSnap.data() ?? {};
    if (flyerData.retailerId !== parsed.data.retailerId) {
      throw new HttpsError('permission-denied', 'Flyer retailer mismatch.');
    }

    // Idempotent replay check: return cleanly without duplicating products
    if (
      flyerData.activePublicationKey === parsed.data.idempotencyKey &&
      flyerData.status === 'published'
    ) {
      return {
        ok: true,
        idempotentReplay: true,
        flyerId: parsed.data.flyerId,
        publicationRevision: flyerData.activePublicationRevision ?? parsed.data.publicationRevision,
        publishedCount: flyerData.publishedProductCount ?? validation.normalizedItems.length
      };
    }

    for (const prod of validation.normalizedItems) {
      const prodRef = db.collection('products').doc(prod.id);
      tx.set(prodRef, {
        ...prod,
        publishedByUid: request.auth!.uid,
        createdAt: now,
        updatedAt: now
      });
    }

    tx.update(flyerRef, {
      status: 'published',
      activePublicationKey: parsed.data.idempotencyKey,
      activePublicationRevision: parsed.data.publicationRevision,
      publishedProductCount: validation.normalizedItems.length,
      publishedAt: now,
      publishedByUid: request.auth!.uid,
      updatedAt: now
    });

    if (parsed.data.jobId) {
      const jobRef = db.collection('extractionJobs').doc(parsed.data.jobId);
      tx.set(
        jobRef,
        {
          state: 'published' as ExtractionJobState,
          publishedCount: validation.normalizedItems.length,
          publishedByUid: request.auth!.uid,
          updatedAt: now
        },
        { merge: true }
      );
    }

    const auditRef = db.collection('auditLogs').doc();
    tx.set(auditRef, {
      collection: 'flyers',
      action: 'PUBLISH_FLYER_ATOMIC',
      documentId: parsed.data.flyerId,
      details: JSON.stringify({
        retailerId: parsed.data.retailerId,
        publicationRevision: parsed.data.publicationRevision,
        idempotencyKey: parsed.data.idempotencyKey,
        publishedCount: validation.normalizedItems.length
      }),
      user: request.auth!.token.email ?? request.auth!.uid,
      actorUid: request.auth!.uid,
      timestamp: now
    });

    return {
      ok: true,
      idempotentReplay: false,
      flyerId: parsed.data.flyerId,
      publicationRevision: parsed.data.publicationRevision,
      publishedCount: validation.normalizedItems.length
    };
  });

  return txResult;
});

export const withdrawFlyer = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required.');
  }
  const parsed = z.object({ flyerId: z.string().min(1).max(128) }).safeParse(request.data);
  if (!parsed.success) {
    throw new HttpsError('invalid-argument', 'Invalid flyerId.');
  }

  const caller = await resolveCallerAuth(request.auth.uid, request.auth.token);
  if (caller.isSuspended) {
    throw new HttpsError('permission-denied', 'Suspended users cannot withdraw flyers.');
  }

  const db = getNamedDb();
  const flyerRef = db.collection('flyers').doc(parsed.data.flyerId);
  const flyerSnap = await flyerRef.get();
  if (!flyerSnap.exists) {
    throw new HttpsError('not-found', 'Flyer not found.');
  }
  const flyerData = flyerSnap.data() ?? {};
  if (!isTenantStaffOrModerator(caller.role, caller.retailerId, String(flyerData.retailerId || ''))) {
    throw new HttpsError('permission-denied', 'Caller cannot withdraw this flyer.');
  }

  const prodSnap = await db
    .collection('products')
    .where('flyerId', '==', parsed.data.flyerId)
    .where('status', '==', 'published')
    .limit(MAX_ATOMIC_PUBLISH_PRODUCTS)
    .get();

  const now = Date.now();
  const batch = db.batch();
  prodSnap.docs.forEach((d) => {
    batch.update(d.ref, { status: 'withdrawn', updatedAt: now });
  });
  batch.update(flyerRef, { status: 'withdrawn', updatedAt: now });
  batch.set(db.collection('auditLogs').doc(), {
    collection: 'flyers',
    action: 'WITHDRAW_FLYER',
    documentId: parsed.data.flyerId,
    details: JSON.stringify({ withdrawnProducts: prodSnap.size }),
    user: request.auth.token.email ?? request.auth.uid,
    actorUid: request.auth.uid,
    timestamp: now
  });
  await batch.commit();

  return { ok: true, withdrawnProducts: prodSnap.size };
});
