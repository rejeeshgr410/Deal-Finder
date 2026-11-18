import { z } from 'zod';

export const CANONICAL_EMIRATES = [
  'Abu Dhabi',
  'Dubai',
  'Sharjah',
  'Ajman',
  'Umm Al Quwain',
  'Ras Al Khaimah',
  'Fujairah',
] as const;

export type CanonicalEmirate = (typeof CANONICAL_EMIRATES)[number];

const EMIRATE_ALIASES: Record<string, CanonicalEmirate> = {
  'abu dhabi': 'Abu Dhabi',
  'أبوظبي': 'Abu Dhabi',
  'ابوظبي': 'Abu Dhabi',
  'أبو ظبي': 'Abu Dhabi',
  'dubai': 'Dubai',
  'دبي': 'Dubai',
  'sharjah': 'Sharjah',
  'الشارقة': 'Sharjah',
  'الشارقه': 'Sharjah',
  'ajman': 'Ajman',
  'عجمان': 'Ajman',
  'umm al quwain': 'Umm Al Quwain',
  'أم القيوين': 'Umm Al Quwain',
  'ام القيوين': 'Umm Al Quwain',
  'ras al khaimah': 'Ras Al Khaimah',
  'رأس الخيمة': 'Ras Al Khaimah',
  'راس الخيمة': 'Ras Al Khaimah',
  'fujairah': 'Fujairah',
  'الفجيرة': 'Fujairah',
  'الفجيره': 'Fujairah',
};

const ARABIC_TO_ENGLISH_ALIASES: Array<[RegExp, string]> = [
  [/حليب/g, 'milk'],
  [/طازج/g, 'fresh'],
  [/كامل الدسم/g, 'full cream'],
  [/قليل الدسم/g, 'low fat'],
  [/خالي الدسم/g, 'skim'],
  [/أرز|ارز|رز/g, 'rice'],
  [/بسمتي/g, 'basmati'],
  [/بيض/g, 'eggs'],
  [/دجاج/g, 'chicken'],
  [/صدور دجاج/g, 'chicken breast'],
  [/زيت/g, 'oil'],
  [/منظف|مسحوق غسيل/g, 'detergent'],
  [/المراعي/g, 'almarai'],
  [/تيلدا/g, 'tilda'],
  [/لتر/g, 'l'],
  [/كيلو|كجم/g, 'kg'],
  [/جرام|جم/g, 'g'],
  [/مل/g, 'ml'],
];

const VARIANT_MARKERS = ['full cream', 'low fat', 'skim', 'zero', 'organic', 'brown', 'basmati', 'breast', 'whole'];

const INJECTION_PATTERNS: RegExp[] = [
  /ignore\s+(all\s+)?(previous|prior|above)\s+instructions?/i,
  /system\s+prompt/i,
  /set\s+role\s*(=|to)\s*(admin|super_admin)/i,
  /grant\s+(me\s+)?admin/i,
  /dump\s+(all\s+)?(users?|database|firestore|tokens?)/i,
  /SELECT\s+\*\s+FROM/i,
  /bypass\s+(security|rules|validation)/i,
  /invent\s+(a\s+)?price/i,
];

export interface AssistantRetailerRecord {
  id: string;
  name: string;
  emirates: string;
  status: string;
}

export interface AssistantOfferRecord {
  id: string;
  retailerId: string;
  name: string;
  brand?: string;
  category?: string;
  barcode?: string;
  masterProductId?: string;
  currentPrice: number;
  amountMinor?: number;
  unitSize?: number;
  unitType?: string;
  packCount?: number;
  validFrom?: number;
  validTo?: number;
  status: string;
  availability?: string;
  loyaltyRequired?: boolean;
  promoConditions?: string;
  sourceType?: string;
  sourceLabel?: string;
  verificationLabel?: string;
  lastVerifiedAt?: number;
  isDemo?: boolean;
}

export interface GroundedOfferMatch {
  offerId: string;
  retailerId: string;
  retailerName: string;
  productName: string;
  brand: string;
  priceFils: number;
  formattedPrice: string;
  unitDisplay: string;
  unitPricePerBaseFils: number | null;
  unitPriceLabel: string | null;
  validTo: string;
  verificationStatus: string;
  sourceLabel: string;
  loyaltyRequired: boolean;
}

export interface BasketStorePlan {
  retailerIds: string[];
  retailerNames: string[];
  matchedCount: number;
  requestedCount: number;
  totalFils: number | null;
  formattedTotal: string | null;
  withinBudget: boolean | null;
  lines: Array<{
    requestedItem: string;
    match: GroundedOfferMatch | null;
  }>;
}

export interface GroundedAssistantResponse {
  language: 'ar' | 'en';
  emirate: CanonicalEmirate | null;
  intent: 'search' | 'unit_compare' | 'basket' | 'source_lookup' | 'rejected';
  answerText: string;
  coverageNote: string;
  matches: GroundedOfferMatch[];
  oneStorePlan: BasketStorePlan | null;
  twoStorePlan: BasketStorePlan | null;
  warnings: string[];
  injectionBlocked: boolean;
}

export const AssistantQueryInputSchema = z.object({
  query: z.string().trim().min(1).max(400),
  emirate: z.string().trim().max(64).optional(),
  shoppingList: z.array(z.string().trim().min(1).max(120)).max(20).optional(),
});

export function formatFils(fils: number): string {
  const neg = fils < 0;
  const abs = Math.abs(Math.round(fils));
  const whole = Math.floor(abs / 100);
  const frac = String(abs % 100).padStart(2, '0');
  return `${neg ? '-' : ''}AED ${whole}.${frac}`;
}

export function effectiveFils(offer: AssistantOfferRecord): number {
  if (typeof offer.amountMinor === 'number' && Number.isFinite(offer.amountMinor) && offer.amountMinor > 0) {
    return Math.round(offer.amountMinor);
  }
  if (Number.isFinite(offer.currentPrice) && offer.currentPrice > 0) {
    return Math.round(offer.currentPrice * 100);
  }
  return 0;
}

export function isArabicText(text: string): boolean {
  return /[\u0600-\u06FF]/.test(text);
}

export function detectPromptInjection(text: string): boolean {
  return INJECTION_PATTERNS.some((re) => re.test(text));
}

export function sanitizeUntrustedText(text: string): string {
  let clean = text.replace(/[\r\n\t]+/g, ' ').trim();
  for (const re of INJECTION_PATTERNS) {
    clean = clean.replace(re, '[redacted]');
  }
  return clean.slice(0, 300);
}

export function resolveEmirate(explicitEmirate?: string, query?: string): CanonicalEmirate | null {
  if (explicitEmirate && explicitEmirate.trim()) {
    const key = explicitEmirate.trim().toLowerCase();
    return EMIRATE_ALIASES[key] ?? null;
  }
  if (query) {
    const lower = query.toLowerCase();
    for (const [alias, canonical] of Object.entries(EMIRATE_ALIASES)) {
      if (lower.includes(alias)) return canonical;
    }
  }
  return null;
}

export function normalizeSearchText(raw: string): string {
  let text = raw.toLowerCase();
  for (const [pattern, replacement] of ARABIC_TO_ENGLISH_ALIASES) {
    text = text.replace(pattern, ` ${replacement} `);
  }
  // Separate numbers from units e.g. 5kg -> 5 kg, 1l -> 1 l
  text = text.replace(/(\d+(?:\.\d+)?)\s*(kg|g|l|ml|pcs?)\b/gi, '$1 $2');
  return text
    .replace(/[^\p{L}\p{N}. ]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function normalizeUnitToBase(unitSize?: number, unitType?: string, packCount = 1): { baseQty: number; baseUnit: 'kg' | 'l' | 'pcs' } | null {
  if (!unitSize || unitSize <= 0 || !unitType) return null;
  const qty = unitSize * Math.max(1, packCount);
  const t = unitType.trim().toLowerCase();
  if (t === 'kg' || t === 'kilogram' || t === 'kilograms') return { baseQty: qty, baseUnit: 'kg' };
  if (t === 'g' || t === 'gm' || t === 'gram' || t === 'grams') return { baseQty: qty / 1000, baseUnit: 'kg' };
  if (t === 'l' || t === 'liter' || t === 'litre' || t === 'liters' || t === 'litres') return { baseQty: qty, baseUnit: 'l' };
  if (t === 'ml' || t === 'milliliter' || t === 'millilitre') return { baseQty: qty / 1000, baseUnit: 'l' };
  if (t === 'pcs' || t === 'pc' || t === 'pack' || t === 'unit' || t === 'eggs') return { baseQty: qty, baseUnit: 'pcs' };
  return null;
}

function computeUnitPrice(offer: AssistantOfferRecord): { perBaseFils: number | null; label: string | null } {
  const fils = effectiveFils(offer);
  const norm = normalizeUnitToBase(offer.unitSize, offer.unitType, offer.packCount ?? 1);
  if (!norm || norm.baseQty <= 0 || fils <= 0) return { perBaseFils: null, label: null };
  const perBaseFils = Math.round(fils / norm.baseQty);
  return { perBaseFils, label: `${formatFils(perBaseFils)} / ${norm.baseUnit}` };
}

export function isOfferAuthorizedAndCurrent(
  offer: AssistantOfferRecord,
  allowedRetailerIds: Set<string>,
  nowMs: number
): boolean {
  if (offer.isDemo === true || offer.sourceType === 'demo') return false;
  if (!offer.status || offer.status.toLowerCase() !== 'published') return false;
  if (!allowedRetailerIds.has(offer.retailerId)) return false;
  if (offer.availability === 'out_of_stock') return false;
  if (effectiveFils(offer) <= 0) return false;
  if (offer.validFrom && offer.validFrom > nowMs) return false;
  if (offer.validTo && offer.validTo > 0 && offer.validTo <= nowMs) return false;
  return true;
}

function toGroundedMatch(offer: AssistantOfferRecord, retailerMap: Map<string, AssistantRetailerRecord>): GroundedOfferMatch {
  const retailer = retailerMap.get(offer.retailerId);
  const fils = effectiveFils(offer);
  const unitInfo = computeUnitPrice(offer);
  const unitDisplay =
    offer.unitSize && offer.unitType
      ? (offer.packCount && offer.packCount > 1 ? `${offer.packCount}x${offer.unitSize} ${offer.unitType}` : `${offer.unitSize} ${offer.unitType}`)
      : 'unit';

  return {
    offerId: offer.id,
    retailerId: offer.retailerId,
    retailerName: sanitizeUntrustedText(retailer?.name || 'Verified Retailer'),
    productName: sanitizeUntrustedText(offer.name),
    brand: sanitizeUntrustedText(offer.brand || ''),
    priceFils: fils,
    formattedPrice: formatFils(fils),
    unitDisplay,
    unitPricePerBaseFils: unitInfo.perBaseFils,
    unitPriceLabel: unitInfo.label,
    validTo: offer.validTo && offer.validTo > 0 ? new Date(offer.validTo).toISOString().slice(0, 10) : 'check_flyer',
    verificationStatus: offer.verificationLabel || 'unverified',
    sourceLabel: sanitizeUntrustedText(offer.sourceLabel || 'Retailer flyer / authorized feed'),
    loyaltyRequired: Boolean(offer.loyaltyRequired),
  };
}

function extractRequestedPack(normalizedQuery: string): { size: number; unit: 'kg' | 'l' | 'pcs' } | null {
  const m = normalizedQuery.match(/\b(\d+(?:\.\d+)?)\s*(kg|g|l|ml|pcs?)\b/i);
  if (!m) return null;
  const rawVal = Number(m[1]);
  const rawUnit = m[2].toLowerCase();
  const base = normalizeUnitToBase(rawVal, rawUnit, 1);
  return base ? { size: base.baseQty, unit: base.baseUnit } : null;
}

function extractVariantMarkers(normalizedText: string): string[] {
  return VARIANT_MARKERS.filter((marker) => normalizedText.includes(marker));
}

/** Tool 1: searchAuthorizedOffers */
export function searchAuthorizedOffers(
  catalog: AssistantOfferRecord[],
  retailers: AssistantRetailerRecord[],
  params: { query: string; emirate: CanonicalEmirate; nowMs?: number }
): { matches: GroundedOfferMatch[]; participatingSources: number } {
  const nowMs = params.nowMs ?? Date.now();
  const activeRetailers = retailers.filter(
    (r) => r.status.toLowerCase() === 'published' && r.emirates.toLowerCase().includes(params.emirate.toLowerCase())
  );
  const retailerMap = new Map(activeRetailers.map((r) => [r.id, r]));
  const allowedIds = new Set(activeRetailers.map((r) => r.id));

  const normQuery = normalizeSearchText(params.query);
  const reqPack = extractRequestedPack(normQuery);
  const reqVariants = extractVariantMarkers(normQuery);

  const stopWords = new Set([
    'find', 'show', 'cheapest', 'exact', 'in', 'the', 'of', 'my', 'for', 'from', 'with', 'which',
    'these', 'two', 'offers', 'offer', 'is', 'cheaper', 'per', 'kilogram', 'liter', 'litre',
    'i', 'have', 'aed', 'currently', 'valid', 'retailers', 'included', 'search', 'and', 'at', 'one', 'store', 'instead',
    'dubai', 'sharjah', 'abu', 'dhabi', 'ajman', 'fujairah', 'ras', 'al', 'khaimah', 'umm', 'quwain',
    'ابحث', 'عن', 'ارخص', 'أرخص', 'في', 'من', 'مع', 'لدي', 'درهم', 'عروض', 'سعر', 'السعر',
    'دبي', 'الشارقة', 'الشارقه', 'أبوظبي', 'ابوظبي', 'أبو', 'ظبي', 'عجمان', 'الفجيرة', 'الفجيره', 'رأس', 'راس', 'الخيمة', 'أم', 'ام', 'القيوين',
  ]);

  const queryTokens = normQuery
    .split(' ')
    .filter((t) => t.length >= 2 && !stopWords.has(t) && !/^\d+(\.\d+)?$/.test(t) && !['kg', 'g', 'l', 'ml', 'pcs', 'pc'].includes(t));

  const matched = catalog
    .filter((offer) => isOfferAuthorizedAndCurrent(offer, allowedIds, nowMs))
    .filter((offer) => {
      const offerNorm = normalizeSearchText(`${offer.name} ${offer.brand || ''} ${offer.category || ''}`);
      // Token match
      if (queryTokens.length > 0 && !queryTokens.every((tok) => offerNorm.includes(tok))) {
        return false;
      }
      // Strict pack-size check if user asked for an exact size (e.g. 1L vs 2L, 5kg vs 1kg)
      if (reqPack) {
        const offerBase = normalizeUnitToBase(offer.unitSize, offer.unitType, offer.packCount ?? 1);
        if (!offerBase || offerBase.baseUnit !== reqPack.unit || Math.abs(offerBase.baseQty - reqPack.size) > 0.001) {
          return false;
        }
      }
      // Strict variant check (e.g. full cream vs skim)
      if (reqVariants.length > 0) {
        const offerVariants = extractVariantMarkers(offerNorm);
        if (!reqVariants.every((v) => offerVariants.includes(v))) {
          return false;
        }
      }
      return true;
    })
    .map((offer) => toGroundedMatch(offer, retailerMap))
    .sort((a, b) => a.priceFils - b.priceFils || a.offerId.localeCompare(b.offerId));

  return { matches: matched, participatingSources: activeRetailers.length };
}

/** Tool 2: resolveProductCandidate */
export function resolveProductCandidate(
  catalog: AssistantOfferRecord[],
  retailers: AssistantRetailerRecord[],
  params: { itemQuery: string; emirate: CanonicalEmirate; nowMs?: number }
): GroundedOfferMatch | null {
  const { matches } = searchAuthorizedOffers(catalog, retailers, {
    query: params.itemQuery,
    emirate: params.emirate,
    nowMs: params.nowMs,
  });
  return matches[0] ?? null;
}

/** Tool 3: calculateBasket (deterministic 1-store vs 2-store integer-fils optimizer) */
export function calculateBasket(
  catalog: AssistantOfferRecord[],
  retailers: AssistantRetailerRecord[],
  params: { items: string[]; emirate: CanonicalEmirate; budgetFils?: number | null; nowMs?: number }
): { oneStorePlan: BasketStorePlan | null; twoStorePlan: BasketStorePlan | null; participatingSources: number } {
  const nowMs = params.nowMs ?? Date.now();
  const activeRetailers = retailers.filter(
    (r) => r.status.toLowerCase() === 'published' && r.emirates.toLowerCase().includes(params.emirate.toLowerCase())
  );
  const retailerMap = new Map(activeRetailers.map((r) => [r.id, r]));

  // Map each requested item -> best offer per retailer
  const itemMatchesByRetailer = params.items.map((item) => {
    const { matches } = searchAuthorizedOffers(catalog, retailers, {
      query: item,
      emirate: params.emirate,
      nowMs,
    });
    const byStore = new Map<string, GroundedOfferMatch>();
    for (const m of matches) {
      const existing = byStore.get(m.retailerId);
      if (!existing || m.priceFils < existing.priceFils) {
        byStore.set(m.retailerId, m);
      }
    }
    return { item, byStore };
  });

  // 1-store evaluation
  let bestOne: BasketStorePlan | null = null;
  for (const retailer of activeRetailers) {
    const lines = itemMatchesByRetailer.map(({ item, byStore }) => ({
      requestedItem: item,
      match: byStore.get(retailer.id) ?? null,
    }));
    const matchedCount = lines.filter((l) => l.match !== null).length;
    if (matchedCount === 0) continue;
    const isComplete = matchedCount === params.items.length;
    const totalFils = isComplete ? lines.reduce((sum, l) => sum + (l.match?.priceFils ?? 0), 0) : null;

    const candidate: BasketStorePlan = {
      retailerIds: [retailer.id],
      retailerNames: [sanitizeUntrustedText(retailer.name)],
      matchedCount,
      requestedCount: params.items.length,
      totalFils,
      formattedTotal: totalFils !== null ? formatFils(totalFils) : null,
      withinBudget: totalFils !== null && params.budgetFils != null ? totalFils <= params.budgetFils : null,
      lines,
    };

    if (
      !bestOne ||
      candidate.matchedCount > bestOne.matchedCount ||
      (candidate.matchedCount === bestOne.matchedCount &&
        candidate.totalFils !== null &&
        (bestOne.totalFils === null || candidate.totalFils < bestOne.totalFils))
    ) {
      bestOne = candidate;
    }
  }

  // 2-store evaluation
  let bestTwo: BasketStorePlan | null = null;
  for (let i = 0; i < activeRetailers.length; i++) {
    for (let j = i + 1; j < activeRetailers.length; j++) {
      const rA = activeRetailers[i];
      const rB = activeRetailers[j];
      const lines = itemMatchesByRetailer.map(({ item, byStore }) => {
        const mA = byStore.get(rA.id) ?? null;
        const mB = byStore.get(rB.id) ?? null;
        if (!mA) return { requestedItem: item, match: mB };
        if (!mB) return { requestedItem: item, match: mA };
        return { requestedItem: item, match: mA.priceFils <= mB.priceFils ? mA : mB };
      });
      const matchedCount = lines.filter((l) => l.match !== null).length;
      if (matchedCount === 0) continue;
      const isComplete = matchedCount === params.items.length;
      const totalFils = isComplete ? lines.reduce((sum, l) => sum + (l.match?.priceFils ?? 0), 0) : null;

      const candidate: BasketStorePlan = {
        retailerIds: [rA.id, rB.id],
        retailerNames: [sanitizeUntrustedText(rA.name), sanitizeUntrustedText(rB.name)],
        matchedCount,
        requestedCount: params.items.length,
        totalFils,
        formattedTotal: totalFils !== null ? formatFils(totalFils) : null,
        withinBudget: totalFils !== null && params.budgetFils != null ? totalFils <= params.budgetFils : null,
        lines,
      };

      if (
        !bestTwo ||
        candidate.matchedCount > bestTwo.matchedCount ||
        (candidate.matchedCount === bestTwo.matchedCount &&
          candidate.totalFils !== null &&
          (bestTwo.totalFils === null || candidate.totalFils < bestTwo.totalFils))
      ) {
        bestTwo = candidate;
      }
    }
  }

  return { oneStorePlan: bestOne, twoStorePlan: bestTwo, participatingSources: retailerMap.size };
}

/** Tool 4: getOfferSource */
export function getOfferSource(
  catalog: AssistantOfferRecord[],
  retailers: AssistantRetailerRecord[],
  offerId: string
): GroundedOfferMatch | null {
  const offer = catalog.find((o) => o.id === offerId);
  if (!offer) return null;
  const retailerMap = new Map(retailers.map((r) => [r.id, r]));
  return toGroundedMatch(offer, retailerMap);
}

function extractBudgetFils(query: string): number | null {
  const m = query.match(/(?:aed|درهم)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:aed|درهم)/i);
  if (!m) return null;
  const num = Number(m[1] || m[2]);
  return Number.isFinite(num) && num > 0 ? Math.round(num * 100) : null;
}

function extractBasketItemsFromQuery(query: string): string[] {
  const norm = normalizeSearchText(query);
  const staples = ['basmati rice', 'rice', 'milk', 'eggs', 'chicken breast', 'chicken', 'oil', 'detergent'];
  const found: string[] = [];
  for (const staple of staples) {
    if (norm.includes(staple) && !found.some((f) => f.includes(staple))) {
      found.push(staple);
    }
  }
  return found;
}

/**
 * Deterministic grounded query orchestrator.
 * Guarantees 0 invented retailer names, prices, or offer IDs.
 */
export function runGroundedAssistantQuery(
  catalog: AssistantOfferRecord[],
  retailers: AssistantRetailerRecord[],
  input: { query: string; emirate?: string; shoppingList?: string[] },
  nowMs: number = Date.now()
): GroundedAssistantResponse {
  const lang: 'ar' | 'en' = isArabicText(input.query) ? 'ar' : 'en';

  // 1. Prompt-injection guardrail on user input
  if (detectPromptInjection(input.query)) {
    return {
      language: lang,
      emirate: null,
      intent: 'rejected',
      answerText:
        lang === 'ar'
          ? 'لا يمكن تنفيذ هذا الطلب. يمكنني فقط البحث في العروض المعتمدة وحساب سلة التسوق.'
          : 'This request cannot be processed. I can only search authorized offers and calculate shopping baskets.',
      coverageNote: 'Request blocked by security guardrails.',
      matches: [],
      oneStorePlan: null,
      twoStorePlan: null,
      warnings: ['prompt_injection_blocked'],
      injectionBlocked: true,
    };
  }

  // 2. Resolve Emirate strictly against UAE allowlist
  const emirate = resolveEmirate(input.emirate, input.query);
  if (!emirate) {
    return {
      language: lang,
      emirate: null,
      intent: 'search',
      answerText:
        lang === 'ar'
          ? 'يرجى تحديد إمارة صالحة في دولة الإمارات (مثل دبي، الشارقة، أبوظبي) لمقارنة العروض المعتمدة.'
          : 'Please specify a valid UAE emirate (e.g., Dubai, Sharjah, Abu Dhabi) so I can compare authorized offers.',
      coverageNote: 'No valid UAE emirate selected.',
      matches: [],
      oneStorePlan: null,
      twoStorePlan: null,
      warnings: ['unknown_or_missing_emirate'],
      injectionBlocked: false,
    };
  }

  const lower = input.query.toLowerCase();
  const budgetFils = extractBudgetFils(input.query);
  const isUnitCompare = /per\s*(kg|kilogram|l|liter|litre)|cheaper\s+per|لكل\s*(كيلو|لتر)/i.test(input.query);
  const explicitList = input.shoppingList && input.shoppingList.length > 0 ? input.shoppingList : null;
  const inferredBasketItems = explicitList ?? (budgetFils !== null || /one store|two store|shopping list|سلة/.test(lower) ? extractBasketItemsFromQuery(input.query) : []);

  // 3. Basket intent
  if (inferredBasketItems.length >= 2 || (explicitList && explicitList.length >= 1)) {
    const { oneStorePlan, twoStorePlan, participatingSources } = calculateBasket(catalog, retailers, {
      items: inferredBasketItems,
      emirate,
      budgetFils,
      nowMs,
    });
    const coverageNote = `Comparing ${participatingSources} verified participating sources in ${emirate}.`;
    const allMatches = [
      ...(oneStorePlan?.lines.map((l) => l.match).filter((m): m is GroundedOfferMatch => m !== null) ?? []),
      ...(twoStorePlan?.lines.map((l) => l.match).filter((m): m is GroundedOfferMatch => m !== null) ?? []),
    ];
    const dedupedMatches = Array.from(new Map(allMatches.map((m) => [m.offerId, m])).values());

    if (dedupedMatches.length === 0) {
      return {
        language: lang,
        emirate,
        intent: 'basket',
        answerText:
          lang === 'ar'
            ? `لم يتم العثور على عروض معتمدة وسارية لهذه القائمة في ${emirate}.`
            : `No authorized current offers were found for these items in ${emirate}.`,
        coverageNote,
        matches: [],
        oneStorePlan: null,
        twoStorePlan: null,
        warnings: ['no_authorized_offers'],
        injectionBlocked: false,
      };
    }

    const bestSummary =
      oneStorePlan?.formattedTotal
        ? `Best 1-store total: ${oneStorePlan.retailerNames[0]} at ${oneStorePlan.formattedTotal} (${oneStorePlan.matchedCount}/${oneStorePlan.requestedCount} items).`
        : oneStorePlan
        ? `Partial 1-store coverage: ${oneStorePlan.retailerNames[0]} matched ${oneStorePlan.matchedCount}/${oneStorePlan.requestedCount} items (incomplete total not estimated).`
        : 'No single store covers these items.';

    return {
      language: lang,
      emirate,
      intent: 'basket',
      answerText: bestSummary,
      coverageNote,
      matches: dedupedMatches,
      oneStorePlan,
      twoStorePlan,
      warnings: oneStorePlan?.totalFils === null ? ['incomplete_basket_coverage'] : [],
      injectionBlocked: false,
    };
  }

  // 4. Unit-price comparison intent
  if (isUnitCompare) {
    const { matches, participatingSources } = searchAuthorizedOffers(catalog, retailers, {
      query: input.query,
      emirate,
      nowMs,
    });
    const withUnit = matches
      .filter((m) => m.unitPricePerBaseFils !== null)
      .sort((a, b) => (a.unitPricePerBaseFils! - b.unitPricePerBaseFils!) || a.offerId.localeCompare(b.offerId));
    const coverageNote = `Comparing ${participatingSources} verified participating sources in ${emirate}.`;

    if (withUnit.length === 0) {
      return {
        language: lang,
        emirate,
        intent: 'unit_compare',
        answerText: `No authorized offers with comparable unit sizes were found in ${emirate}.`,
        coverageNote,
        matches: [],
        oneStorePlan: null,
        twoStorePlan: null,
        warnings: ['no_comparable_units'],
        injectionBlocked: false,
      };
    }

    const winner = withUnit[0];
    return {
      language: lang,
      emirate,
      intent: 'unit_compare',
      answerText: `${winner.productName} at ${winner.retailerName} is cheapest per unit at ${winner.unitPriceLabel} (${winner.formattedPrice} for ${winner.unitDisplay}).`,
      coverageNote,
      matches: withUnit,
      oneStorePlan: null,
      twoStorePlan: null,
      warnings: [],
      injectionBlocked: false,
    };
  }

  // 5. Standard authorized search intent
  const { matches, participatingSources } = searchAuthorizedOffers(catalog, retailers, {
    query: input.query,
    emirate,
    nowMs,
  });
  const coverageNote = `Comparing ${participatingSources} verified participating sources in ${emirate}.`;

  if (matches.length === 0) {
    return {
      language: lang,
      emirate,
      intent: 'search',
      answerText:
        lang === 'ar'
          ? `لا توجد عروض معتمدة وسارية مطابقة لبحثك في ${emirate}.`
          : `No authorized current offers match your request in ${emirate}.`,
      coverageNote,
      matches: [],
      oneStorePlan: null,
      twoStorePlan: null,
      warnings: ['no_authorized_offers'],
      injectionBlocked: false,
    };
  }

  const top = matches[0];
  return {
    language: lang,
    emirate,
    intent: 'search',
    answerText:
      lang === 'ar'
        ? `أرخص عرض معتمد في ${emirate}: ${top.productName} لدى ${top.retailerName} بسعر ${top.formattedPrice} (${top.unitDisplay}).`
        : `Cheapest authorized offer in ${emirate}: ${top.productName} at ${top.retailerName} for ${top.formattedPrice} (${top.unitDisplay}).`,
    coverageNote,
    matches,
    oneStorePlan: null,
    twoStorePlan: null,
    warnings: [],
    injectionBlocked: false,
  };
}
