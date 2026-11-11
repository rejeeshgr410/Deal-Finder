/**
 * Product Catalog Matching & Deduplication Engine
 */

export interface MatchResult {
  masterId: string | null;
  confidence: number; // 0 to 100
  matchType: 'BARCODE' | 'ALIAS' | 'EXACT_NORMALIZED' | 'BRAND_UNIT' | 'FUZZY_TEXT' | 'NONE';
}

/**
 * Normalizes strings by lowercasing, stripping punctuation, and compressing whitespace.
 */
export function normalizeString(str: string): string {
  if (!str) return '';
  return str
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Extracted volume size (number) and volume unit (ml, l, kg, g, etc.) from a name string
 */
export interface ExtractedUnit {
  size: number | null;
  type: string | null;
}

export function extractUnit(name: string): ExtractedUnit {
  const normalized = name.toLowerCase();
  
  // Look for patterns like "330 ml", "330ml", "1.5 kg", "1.5kg", "500g", "500 g", "1.5l", "1.5 l", "1.5 liter"
  const patterns = [
    /(\d+(?:\.\d+)?)\s*(ml|l|g|kg|pcs|can|pack|ltr|liter|grams|kilogram)/i,
    /(\d+(?:\.\d+)?)\s*(ounces|oz)/i
  ];

  for (const pattern of patterns) {
    const match = normalized.match(pattern);
    if (match) {
      let unitType = match[2].toLowerCase();
      // standardize typical unit abbreviations
      if (unitType === 'ltr' || unitType === 'liter') unitType = 'l';
      if (unitType === 'grams') unitType = 'g';
      if (unitType === 'kilogram') unitType = 'kg';
      
      return {
        size: parseFloat(match[1]),
        type: unitType
      };
    }
  }

  return { size: null, type: null };
}

/**
 * Calculate Jaccard similarity coefficient between two word sets.
 */
export function calculateWordOverlap(str1: string, str2: string): number {
  const norm1 = normalizeString(str1);
  const norm2 = normalizeString(str2);
  
  if (!norm1 || !norm2) return 0;
  
  const words1 = new Set(norm1.split(' ').filter(w => w.length > 1));
  const words2 = new Set(norm2.split(' ').filter(w => w.length > 1));
  
  if (words1.size === 0 || words2.size === 0) return 0;
  
  const intersection = new Set([...words1].filter(x => words2.has(x)));
  const union = new Set([...words1, ...words2]);
  
  return intersection.size / union.size;
}

/**
 * Levenshtein Distance similarity
 */
export function calculateLevenshteinSimilarity(s1: string, s2: string): number {
  const longer = s1.length > s2.length ? s1 : s2;
  const shorter = s1.length > s2.length ? s2 : s1;
  const longerLength = longer.length;
  
  if (longerLength === 0) {
    return 1.0;
  }
  
  const costs = new Array(shorter.length + 1);
  for (let i = 0; i <= shorter.length; i++) {
    costs[i] = i;
  }
  
  for (let i = 1; i <= longer.length; i++) {
    let dw = i - 1;
    costs[0] = i;
    for (let j = 1; j <= shorter.length; j++) {
      const c = longer[i - 1] === shorter[j - 1] ? 0 : 1;
      const nw = costs[j];
      costs[j] = Math.min(costs[j] + 1, costs[j - 1] + 1, dw + c);
      dw = nw;
    }
  }
  
  return (longerLength - costs[shorter.length]) / longerLength;
}

/**
 * Match a product proposal with the best option in the master list.
 */
export function findBestMasterMatch(
  proposed: {
    name: string;
    barcode?: string;
    brand?: string;
    unitSize?: string;
    unitType?: string;
    category?: string;
  },
  masters: any[]
): MatchResult {
  if (!masters || masters.length === 0) {
    return { masterId: null, confidence: 0, matchType: 'NONE' };
  }

  const proposedName = proposed.name;
  const proposedBarcode = proposed.barcode ? proposed.barcode.trim() : '';
  const proposedBrand = proposed.brand ? proposed.brand.trim().toLowerCase() : '';
  const propUnit = extractUnit(proposedName);
  
  const propUnitSize = proposed.unitSize ? parseFloat(proposed.unitSize) : propUnit.size;
  const propUnitType = proposed.unitType ? proposed.unitType.trim().toLowerCase() : propUnit.type;

  const normalizedProposedName = normalizeString(proposedName);

  let bestMatch: any = null;
  let bestConfidence = 0;
  let bestType: MatchResult['matchType'] = 'NONE';

  for (const master of masters) {
    // 1. BARCODE MATCH
    const masterBarcode = master.barcode ? master.barcode.trim() : '';
    if (proposedBarcode && masterBarcode && proposedBarcode === masterBarcode && proposedBarcode.length > 2) {
      return { masterId: master.id, confidence: 100, matchType: 'BARCODE' };
    }

    // Prepare master values for comparisons
    const masterCanonical = master.canonicalName || '';
    const normalizedMasterCanonical = normalizeString(masterCanonical);
    const masterBrand = master.brand ? master.brand.trim().toLowerCase() : '';
    
    const masterUnitSize = master.unitSize ? parseFloat(master.unitSize) : null;
    const masterUnitType = master.unitType ? master.unitType.trim().toLowerCase() : null;

    // 2. EXACT NORMALIZED MATCH
    if (normalizedProposedName === normalizedMasterCanonical) {
      if (bestConfidence < 95) {
        bestConfidence = 95;
        bestMatch = master;
        bestType = 'EXACT_NORMALIZED';
      }
    }

    // 3. ALIAS MATCH
    if (master.aliases && Array.isArray(master.aliases)) {
      const isAliasMatch = master.aliases.some((alias: any) => {
        const normAlias = normalizeString(String(alias));
        return normAlias === normalizedProposedName || normalizedProposedName.includes(normAlias);
      });
      if (isAliasMatch) {
        if (bestConfidence < 92) {
          bestConfidence = 92;
          bestMatch = master;
          bestType = 'ALIAS';
        }
      }
    }

    // 4. BRAND + UNIT SIZE + UNIT TYPE MATCH
    const brandMatches = proposedBrand && masterBrand && (proposedBrand === masterBrand || normalizedProposedName.includes(masterBrand));
    const unitMatches = propUnitSize && masterUnitSize && propUnitSize === masterUnitSize && propUnitType === masterUnitType;
    
    if (brandMatches && unitMatches) {
      if (bestConfidence < 90) {
        bestConfidence = 90;
        bestMatch = master;
        bestType = 'BRAND_UNIT';
      }
    }

    // 5. FUZZY TEXT MATCH
    const overlapScore = calculateWordOverlap(proposedName, masterCanonical);
    const levScore = calculateLevenshteinSimilarity(normalizedProposedName, normalizedMasterCanonical);
    const combinedFuzzy = Math.max(overlapScore, levScore) * 100;

    if (combinedFuzzy > 65) {
      let confidenceBonus = 0;
      if (proposedBrand && masterBrand && (proposedBrand === masterBrand)) {
        confidenceBonus += 10;
      }
      if (propUnitSize && masterUnitSize && propUnitSize === masterUnitSize) {
        confidenceBonus += 10;
      }
      
      const finalFuzzyScore = Math.min(combinedFuzzy + confidenceBonus, 89);
      if (finalFuzzyScore > bestConfidence) {
        bestConfidence = Math.round(finalFuzzyScore);
        bestMatch = master;
        bestType = 'FUZZY_TEXT';
      }
    }
  }

  // If we found a candidate with confidence above 55
  if (bestMatch && bestConfidence >= 55) {
    return {
      masterId: bestMatch.id,
      confidence: bestConfidence,
      matchType: bestType
    };
  }

  return { masterId: null, confidence: 0, matchType: 'NONE' };
}
