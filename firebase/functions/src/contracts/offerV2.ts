export type OfferSource = 'merchant_upload' | 'official_feed' | 'licensed_import' | 'demo';

export type ReviewStatus =
  | 'draft'
  | 'extracting'
  | 'review_required'
  | 'approved'
  | 'published'
  | 'rejected'
  | 'expired'
  | 'withdrawn';

export type BaseUnit = 'g' | 'ml' | 'each';

export type AvailabilityStatus = 'unknown' | 'retailer_reported' | 'out_of_stock';

export type VerificationLabel = 'merchant_confirmed' | 'admin_reviewed' | 'unverified';

export type VerificationMethod = 'merchant_review' | 'admin_review' | 'licensed_feed';

export type OfferIdentity = {
  masterProductId?: string;
  barcodeGtin?: string;
  brand: string;
  nameEn: string;
  nameAr?: string;
  variant?: string;
  baseQuantity?: number;
  baseUnit?: BaseUnit;
  packCount?: number;
};

export type PriceTerms = {
  currency: 'AED';
  amountMinor: number;
  comparisonPriceMinor?: number;
  minUnits?: number;
  buyUnits?: number;
  freeUnits?: number;
  loyaltyRequired?: boolean;
  couponRequired?: boolean;
  redemptionLimit?: number;
  validFrom: string;
  validTo: string;
  availability: AvailabilityStatus;
};

export type Provenance = {
  sourceType: OfferSource;
  retailerId: string;
  branchIds?: string[];
  sourceAssetId: string;
  sourcePage: number;
  sourceBox?: [number, number, number, number];
  permissionBasisRef?: string;
  extractedAt: string;
  verifiedAt?: string;
  verifiedBy?: string;
  verificationMethod?: VerificationMethod;
  modelName?: string;
  modelTemplateRevision?: string;
  importRunId?: string;
};

export type PublicOfferV2 = {
  schemaVersion: 2;
  id: string;
  retailerId: string;
  identity: OfferIdentity;
  terms: PriceTerms;
  provenancePublic: {
    sourceLabel: string;
    sourcePage?: number;
    offerValidTo: string;
    lastVerifiedAt?: string;
    verificationLabel: VerificationLabel;
  };
  status: 'published' | 'expired' | 'withdrawn';
  dedupeKey: string;
  updatedAt: string;
};

export type ExtractedField<T> = {
  value: T | null;
  rawText?: string;
  sourcePage?: number;
  sourceBox?: [number, number, number, number];
  reviewReason?: string[];
};

export const UAE_LAT_MIN = 22.5;
export const UAE_LAT_MAX = 26.5;
export const UAE_LON_MIN = 51.0;
export const UAE_LON_MAX = 56.6;
export const MAX_OFFER_AMOUNT_MINOR = 5_000_000; // AED 50,000.00 guardrail

export function aedToFils(amountAed: number): number {
  if (!Number.isFinite(amountAed) || amountAed < 0) {
    throw new Error(`Invalid AED amount: ${amountAed}`);
  }
  return Math.round(amountAed * 100);
}

export function isValidGtin(raw: string | null | undefined): boolean {
  if (!raw) return false;
  const digits = raw.trim();
  if (!/^\d{8}$|^\d{12}$|^\d{13}$|^\d{14}$/.test(digits)) return false;
  // Reject all-zero or repeating placeholder barcodes
  if (/^0+$/.test(digits)) return false;

  const body = digits.slice(0, -1);
  const expectedCheck = Number(digits.slice(-1));
  let sum = 0;
  for (let i = 0; i < body.length; i++) {
    const digit = Number(body[body.length - 1 - i]);
    sum += digit * (i % 2 === 0 ? 3 : 1);
  }
  const computed = (10 - (sum % 10)) % 10;
  return computed === expectedCheck;
}

export function normalizeQuantityUnit(
  rawSize: number | string | null | undefined,
  rawUnit: string | null | undefined,
  rawPackCount?: number | null
): { baseQuantity: number; baseUnit: BaseUnit; packCount: number } | null {
  let packCount = rawPackCount && Number.isInteger(rawPackCount) && rawPackCount >= 1 ? rawPackCount : 1;

  // Support strings like "6x330" or "6 x 330"
  let numericSize = 0;
  if (typeof rawSize === 'number') {
    numericSize = rawSize;
  } else if (typeof rawSize === 'string') {
    const packMatch = rawSize.trim().match(/^(\d+)\s*[xX×]\s*(\d+(?:\.\d+)?)$/);
    if (packMatch) {
      packCount = Math.max(1, parseInt(packMatch[1], 10));
      numericSize = parseFloat(packMatch[2]);
    } else {
      numericSize = parseFloat(rawSize.trim());
    }
  }

  if (!Number.isFinite(numericSize) || numericSize <= 0) return null;

  const unit = (rawUnit || '').trim().toLowerCase();
  if (!unit) return null;

  if (unit === 'g' || unit === 'gm' || unit === 'gram' || unit === 'grams') {
    return { baseQuantity: Math.round(numericSize * 1000) / 1000, baseUnit: 'g', packCount };
  }
  if (unit === 'kg' || unit === 'kgs' || unit === 'kilogram' || unit === 'kilograms') {
    return { baseQuantity: Math.round(numericSize * 1000 * 1000) / 1000, baseUnit: 'g', packCount };
  }
  if (unit === 'ml') {
    return { baseQuantity: Math.round(numericSize * 1000) / 1000, baseUnit: 'ml', packCount };
  }
  if (unit === 'l' || unit === 'ltr' || unit === 'liter' || unit === 'liters' || unit === 'litre' || unit === 'litres') {
    return { baseQuantity: Math.round(numericSize * 1000 * 1000) / 1000, baseUnit: 'ml', packCount };
  }
  if (
    unit === 'each' ||
    unit === 'ea' ||
    unit === 'pc' ||
    unit === 'pcs' ||
    unit === 'piece' ||
    unit === 'pieces' ||
    unit === 'unit' ||
    unit === 'can' ||
    unit === 'cans' ||
    unit === 'bottle' ||
    unit === 'bottles' ||
    unit === 'pack' ||
    unit === 'packs'
  ) {
    return { baseQuantity: Math.round(numericSize * 1000) / 1000, baseUnit: 'each', packCount };
  }

  return null;
}

export function isGuessedDefaultBox(box: number[] | undefined | null): boolean {
  if (!box || box.length !== 4) return false;
  return box[0] === 100 && box[1] === 100 && box[2] === 400 && box[3] === 400;
}

export function isValidSourceBox(box: number[] | undefined | null): boolean {
  if (!box || box.length !== 4) return false;
  if (isGuessedDefaultBox(box)) return false;
  const [ymin, xmin, ymax, xmax] = box;
  if ([ymin, xmin, ymax, xmax].some((n) => !Number.isFinite(n))) return false;
  if (ymin < 0 || xmin < 0 || ymax > 1000 || xmax > 1000) return false;
  return ymax > ymin && xmax > xmin;
}

export type BranchCoordinateCheck = {
  valid: boolean;
  latitude: number | null;
  longitude: number | null;
  coordsVerified: boolean;
  errors: string[];
};

const VERIFIED_COORD_SOURCES = new Set(['verified_branch_source', 'merchant_entered', 'official_feed']);

export function validateBranchCoordinates(
  rawLat: unknown,
  rawLon: unknown,
  coordSource?: string
): BranchCoordinateCheck {
  const errors: string[] = [];
  const lat = typeof rawLat === 'number' ? rawLat : parseFloat(String(rawLat ?? ''));
  const lon = typeof rawLon === 'number' ? rawLon : parseFloat(String(rawLon ?? ''));

  if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
    errors.push('Coordinates must be finite numbers.');
    return { valid: false, latitude: null, longitude: null, coordsVerified: false, errors };
  }
  if (lat === 0 && lon === 0) {
    errors.push('Coordinates (0, 0) are invalid.');
    return { valid: false, latitude: null, longitude: null, coordsVerified: false, errors };
  }
  if (lat < UAE_LAT_MIN || lat > UAE_LAT_MAX || lon < UAE_LON_MIN || lon > UAE_LON_MAX) {
    errors.push(`Coordinates (${lat}, ${lon}) fall outside the UAE geographic bounding box.`);
    return { valid: false, latitude: null, longitude: null, coordsVerified: false, errors };
  }

  const coordsVerified = Boolean(coordSource && VERIFIED_COORD_SOURCES.has(coordSource));
  return { valid: true, latitude: lat, longitude: lon, coordsVerified, errors };
}

export type PublishCandidate = {
  retailerId: string;
  nameEn: string;
  brand?: string;
  barcodeGtin?: string;
  amountMinor: number;
  comparisonPriceMinor?: number | null;
  currency?: string;
  baseQuantity?: number | null;
  baseUnit?: BaseUnit | null;
  packCount?: number | null;
  minUnits?: number | null;
  buyUnits?: number | null;
  freeUnits?: number | null;
  validFromMs: number;
  validToMs: number;
  sourceType: OfferSource;
  sourceAssetId: string;
  sourceBox?: [number, number, number, number] | null;
  verificationMethod?: VerificationMethod | null;
  availability?: AvailabilityStatus;
  allowDemo?: boolean;
};

export function validateOfferForPublish(
  candidate: PublishCandidate,
  nowMs: number = Date.now()
): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (!candidate.retailerId || !candidate.retailerId.trim()) {
    errors.push('MISSING_RETAILER_ID');
  }
  if (!candidate.nameEn || candidate.nameEn.trim().length < 2) {
    errors.push('INVALID_PRODUCT_NAME');
  }
  if ((candidate.currency ?? 'AED') !== 'AED') {
    errors.push('UNSUPPORTED_CURRENCY');
  }
  if (!Number.isInteger(candidate.amountMinor) || candidate.amountMinor <= 0 || candidate.amountMinor > MAX_OFFER_AMOUNT_MINOR) {
    errors.push('INVALID_PRICE_MINOR');
  }
  if (
    candidate.comparisonPriceMinor !== undefined &&
    candidate.comparisonPriceMinor !== null &&
    (!Number.isInteger(candidate.comparisonPriceMinor) || candidate.comparisonPriceMinor <= candidate.amountMinor)
  ) {
    errors.push('INVALID_COMPARISON_PRICE');
  }
  if (!candidate.baseQuantity || candidate.baseQuantity <= 0 || !candidate.baseUnit) {
    errors.push('UNIT_OR_SIZE_UNSPECIFIED');
  }
  if (candidate.packCount !== undefined && candidate.packCount !== null && (!Number.isInteger(candidate.packCount) || candidate.packCount < 1)) {
    errors.push('INVALID_PACK_COUNT');
  }
  if (candidate.barcodeGtin && !isValidGtin(candidate.barcodeGtin)) {
    errors.push('INVALID_BARCODE_GTIN');
  }
  if (!Number.isFinite(candidate.validFromMs) || !Number.isFinite(candidate.validToMs) || candidate.validFromMs >= candidate.validToMs) {
    errors.push('INVALID_VALIDITY_WINDOW');
  } else if (candidate.validToMs <= nowMs) {
    errors.push('OFFER_ALREADY_EXPIRED');
  }
  if (candidate.sourceType === 'demo' && !candidate.allowDemo) {
    errors.push('DEMO_SOURCE_FORBIDDEN_IN_PUBLISH');
  }
  if (!candidate.sourceAssetId || !candidate.sourceAssetId.trim()) {
    errors.push('MISSING_SOURCE_ASSET');
  }
  if (candidate.sourceBox && !isValidSourceBox(candidate.sourceBox)) {
    errors.push('INVALID_OR_GUESSED_SOURCE_BOX');
  }
  if (!candidate.verificationMethod) {
    errors.push('MISSING_VERIFICATION_RECEIPT');
  }
  if ((candidate.buyUnits && !candidate.freeUnits) || (!candidate.buyUnits && candidate.freeUnits)) {
    errors.push('INCOMPLETE_BOGO_TERMS');
  }
  if (candidate.minUnits !== undefined && candidate.minUnits !== null && (!Number.isInteger(candidate.minUnits) || candidate.minUnits < 1)) {
    errors.push('INVALID_MIN_UNITS');
  }

  return { valid: errors.length === 0, errors };
}

export function buildDedupeKey(retailerId: string, identity: Pick<OfferIdentity, 'masterProductId' | 'barcodeGtin' | 'brand' | 'nameEn' | 'baseQuantity' | 'baseUnit' | 'packCount'>): string {
  if (identity.barcodeGtin && isValidGtin(identity.barcodeGtin)) {
    return `${retailerId}::gtin::${identity.barcodeGtin}::p${identity.packCount ?? 1}`;
  }
  if (identity.masterProductId && identity.masterProductId.trim()) {
    return `${retailerId}::master::${identity.masterProductId.trim()}`;
  }
  const norm = `${identity.brand} ${identity.nameEn}`
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  return `${retailerId}::norm::${norm}::${identity.baseQuantity ?? 0}${identity.baseUnit ?? ''}::p${identity.packCount ?? 1}`;
}

export function mapV1ProductToV2Patch(
  docId: string,
  v1: Record<string, any>,
  importRunId: string
): Record<string, any> {
  const currentPrice = typeof v1.currentPrice === 'number' ? v1.currentPrice : parseFloat(String(v1.currentPrice ?? 0)) || 0;
  const oldPrice = typeof v1.oldPrice === 'number' ? v1.oldPrice : parseFloat(String(v1.oldPrice ?? 0)) || 0;
  const amountMinor = currentPrice > 0 ? aedToFils(currentPrice) : 0;
  const comparisonPriceMinor = oldPrice > currentPrice && oldPrice > 0 ? aedToFils(oldPrice) : null;

  const normalizedUnit = normalizeQuantityUnit(v1.unitSize, v1.unitType, v1.packCount);
  const validFromMs = typeof v1.validFrom === 'number' && v1.validFrom > 0 ? v1.validFrom : 0;
  const validToMs = typeof v1.validTo === 'number' && v1.validTo > 0 ? v1.validTo : 0;

  const hasVerifiedOrigin = Boolean(v1.permissionBasisRef || v1.sourceAssetId);
  const isDemo = Boolean(v1.isDemo || v1.sourceType === 'demo' || String(v1.imageUrl || '').startsWith('file:///android_asset/'));
  const sourceType: OfferSource = isDemo
    ? 'demo'
    : (v1.sourceType as OfferSource) || 'merchant_upload';

  const verificationLabel: VerificationLabel =
    v1.verificationLabel ||
    (v1.verificationMethod === 'merchant_review'
      ? 'merchant_confirmed'
      : v1.verificationMethod === 'admin_review'
        ? 'admin_reviewed'
        : 'unverified');

  const rightsStatus = isDemo
    ? 'DEMO_ONLY'
    : hasVerifiedOrigin
      ? 'VERIFIED'
      : 'UNVERIFIED';

  const dedupeKey = buildDedupeKey(String(v1.retailerId || 'unknown'), {
    masterProductId: v1.masterProductId || undefined,
    barcodeGtin: isValidGtin(v1.barcode) ? String(v1.barcode).trim() : undefined,
    brand: String(v1.brand || ''),
    nameEn: String(v1.name || docId),
    baseQuantity: normalizedUnit?.baseQuantity,
    baseUnit: normalizedUnit?.baseUnit,
    packCount: normalizedUnit?.packCount
  });

  return {
    schemaVersion: 2,
    currency: 'AED',
    amountMinor,
    comparisonPriceMinor,
    baseQuantity: normalizedUnit?.baseQuantity ?? null,
    baseUnit: normalizedUnit?.baseUnit ?? null,
    packCount: normalizedUnit?.packCount ?? 1,
    barcodeGtin: isValidGtin(v1.barcode) ? String(v1.barcode).trim() : null,
    validFromIso: validFromMs > 0 ? new Date(validFromMs).toISOString() : null,
    validToIso: validToMs > 0 ? new Date(validToMs).toISOString() : null,
    availability: (v1.availability as AvailabilityStatus) || 'unknown',
    sourceType,
    sourceLabel: isDemo ? 'Demo Sample Asset' : (v1.sourceLabel || 'Merchant Upload'),
    verificationLabel,
    rightsStatus,
    isDemo,
    dedupeKey,
    importRunId
  };
}
