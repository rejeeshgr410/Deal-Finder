import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateBranchCoordinates } from '../firebase/functions/src/contracts/offerV2';

export interface AuditReport {
  timestamp: string;
  mode: 'snapshot' | 'd4d_local';
  totals: {
    branches: number;
    products: number;
    flyers: number;
  };
  issues: {
    invalidOrUnverifiedBranches: number;
    expiredPublishedProducts: number;
    missingProvenanceOrRights: number;
    demoOrBundledAssetProducts: number;
    invalidOldPriceSemantics: number;
  };
  sampleFindings: string[];
}

export function auditDataset(
  input: { branches?: any[]; products?: any[]; flyers?: any[] },
  mode: 'snapshot' | 'd4d_local' = 'snapshot',
  nowMs: number = Date.now()
): AuditReport {
  const branches = input.branches ?? [];
  const products = input.products ?? [];
  const flyers = input.flyers ?? [];
  const sampleFindings: string[] = [];

  let invalidOrUnverifiedBranches = 0;
  for (const b of branches) {
    const check = validateBranchCoordinates(b.latitude, b.longitude, b.coordSource);
    if (!check.valid || !b.coordsVerified) {
      invalidOrUnverifiedBranches++;
      if (sampleFindings.length < 15) {
        sampleFindings.push(`BRANCH_UNVERIFIED:${b.id || b.name || 'unknown'}:${check.errors[0] || 'coordsVerified!=true'}`);
      }
    }
  }

  let expiredPublishedProducts = 0;
  let missingProvenanceOrRights = 0;
  let demoOrBundledAssetProducts = 0;
  let invalidOldPriceSemantics = 0;

  for (const p of products) {
    if (String(p.status || '').toLowerCase() === 'published' && typeof p.validTo === 'number' && p.validTo > 0 && p.validTo <= nowMs) {
      expiredPublishedProducts++;
    }
    if (!p.permissionBasisRef && !p.sourceAssetId) {
      missingProvenanceOrRights++;
    }
    if (p.isDemo === true || p.sourceType === 'demo' || String(p.imageUrl || '').startsWith('file:///android_asset/')) {
      demoOrBundledAssetProducts++;
    }
    if (typeof p.oldPrice === 'number' && p.oldPrice > 0 && typeof p.currentPrice === 'number' && p.oldPrice <= p.currentPrice) {
      invalidOldPriceSemantics++;
    }
  }

  return {
    timestamp: new Date(nowMs).toISOString(),
    mode,
    totals: {
      branches: branches.length,
      products: products.length,
      flyers: flyers.length
    },
    issues: {
      invalidOrUnverifiedBranches,
      expiredPublishedProducts,
      missingProvenanceOrRights,
      demoOrBundledAssetProducts,
      invalidOldPriceSemantics
    },
    sampleFindings
  };
}

function main() {
  const args = process.argv.slice(2);
  const snapshotFlagIdx = args.indexOf('--snapshot');
  const __dirname = path.dirname(fileURLToPath(import.meta.url));
  const repoRoot = path.resolve(__dirname, '..');

  if (snapshotFlagIdx !== -1 && args[snapshotFlagIdx + 1]) {
    const snapshotPath = path.resolve(process.cwd(), args[snapshotFlagIdx + 1]);
    const raw = JSON.parse(fs.readFileSync(snapshotPath, 'utf8'));
    const report = auditDataset(raw, 'snapshot');
    console.log(JSON.stringify(report, null, 2));
    return;
  }

  // Default read-only inspection of local D4D catalogue/shop files if present
  const d4dCataloguesPath = path.join(repoRoot, 'D4D', 'catalogues.json');
  const d4dShopsPath = path.join(repoRoot, 'D4D', 'shops.json');
  const shops = fs.existsSync(d4dShopsPath) ? JSON.parse(fs.readFileSync(d4dShopsPath, 'utf8')) : [];
  const flyers = fs.existsSync(d4dCataloguesPath) ? JSON.parse(fs.readFileSync(d4dCataloguesPath, 'utf8')) : [];

  const syntheticBranches = shops.flatMap((s: any) =>
    (s.branches && s.branches.length > 0 ? s.branches : [s.name]).map((b: string, idx: number) => ({
      id: `d4d-${s.id}-${idx + 1}`,
      name: b,
      latitude: null,
      longitude: null,
      coordsVerified: false,
      coordSource: 'unverified_d4d'
    }))
  );

  const report = auditDataset({ branches: syntheticBranches, flyers, products: [] }, 'd4d_local');
  console.log(JSON.stringify(report, null, 2));
}

const isEntrypoint = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isEntrypoint) {
  main();
}
