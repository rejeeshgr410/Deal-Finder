import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { GeminiProductRawSchema, sanitizeExtractionItem } from '../firebase/functions/src/gemini';
import { aedToFils, normalizeQuantityUnit, buildDedupeKey } from '../firebase/functions/src/contracts/offerV2';

export interface BenchmarkMetrics {
  fixtureType: string;
  evaluatedAt: string;
  totalRows: number;
  eligiblePublishedCheckRows: number;
  productNameExactRate: number;
  priceExactRate: number;
  sizeAndPackExactRate: number;
  promoConditionExactRate: number;
  falseDuplicateRate: number;
  flaggedInvalidRowsCaught: number;
}

export function runExtractionBenchmark(fixturePath: string): BenchmarkMetrics {
  const fixture = JSON.parse(fs.readFileSync(fixturePath, 'utf8'));
  const rows: any[] = fixture.rows || [];

  let nameMatches = 0;
  let priceMatches = 0;
  let sizePackMatches = 0;
  let promoMatches = 0;
  let flaggedInvalidRowsCaught = 0;

  const dedupeKeys = new Set<string>();
  let falseDuplicates = 0;

  for (const row of rows) {
    const parsedRaw = GeminiProductRawSchema.parse(row.rawModelOutput);
    const sanitized = sanitizeExtractionItem(parsedRaw, 1);
    const exp = row.expected;

    if (row.expectFlaggedForReview) {
      if (sanitized.reviewReasons.length > 0) {
        flaggedInvalidRowsCaught++;
      }
      continue;
    }

    if (sanitized.name === exp.name) nameMatches++;
    if (aedToFils(sanitized.currentPrice) === aedToFils(exp.currentPrice)) priceMatches++;

    const normGot = normalizeQuantityUnit(sanitized.unitSize, sanitized.unitType);
    const normExp = normalizeQuantityUnit(exp.unitSize, exp.unitType, exp.packCount);
    if (
      normGot &&
      normExp &&
      normGot.baseQuantity === normExp.baseQuantity &&
      normGot.baseUnit === normExp.baseUnit &&
      normGot.packCount === normExp.packCount
    ) {
      sizePackMatches++;
    }

    if (sanitized.promoConditions === exp.promoConditions) promoMatches++;

    const dKey = buildDedupeKey('benchmark-retailer', {
      brand: sanitized.brand,
      nameEn: sanitized.name,
      baseQuantity: normGot?.baseQuantity,
      baseUnit: normGot?.baseUnit,
      packCount: normGot?.packCount
    });
    if (dedupeKeys.has(dKey)) {
      falseDuplicates++;
    } else {
      dedupeKeys.add(dKey);
    }
  }

  const eligible = rows.filter((r) => !r.expectFlaggedForReview).length || 1;
  return {
    fixtureType: fixture.fixtureType || 'SYNTHETIC_TEST_FIXTURE',
    evaluatedAt: new Date().toISOString(),
    totalRows: rows.length,
    eligiblePublishedCheckRows: eligible,
    productNameExactRate: Number((nameMatches / eligible).toFixed(4)),
    priceExactRate: Number((priceMatches / eligible).toFixed(4)),
    sizeAndPackExactRate: Number((sizePackMatches / eligible).toFixed(4)),
    promoConditionExactRate: Number((promoMatches / eligible).toFixed(4)),
    falseDuplicateRate: Number((falseDuplicates / eligible).toFixed(4)),
    flaggedInvalidRowsCaught
  };
}

function main() {
  const __dirname = path.dirname(fileURLToPath(import.meta.url));
  const defaultFixture = path.resolve(__dirname, '..', 'test-fixtures', 'annotations', 'uae_flyer_benchmark_v1.json');
  const metrics = runExtractionBenchmark(defaultFixture);
  console.log(JSON.stringify(metrics, null, 2));
}

const isEntrypoint = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isEntrypoint) {
  main();
}
