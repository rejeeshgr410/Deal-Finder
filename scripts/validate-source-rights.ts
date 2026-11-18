import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export interface SourceRightsEntry {
  sourceName: string;
  recordCount: number;
  classification: 'AUTHORIZED' | 'DEMO_ONLY' | 'NOT_VERIFIED';
  reason: string;
}

export function evaluateSourceRights(repoRoot: string): SourceRightsEntry[] {
  const entries: SourceRightsEntry[] = [];

  const cataloguesPath = path.join(repoRoot, 'D4D', 'catalogues.json');
  if (fs.existsSync(cataloguesPath)) {
    const catalogues = JSON.parse(fs.readFileSync(cataloguesPath, 'utf8'));
    entries.push({
      sourceName: 'D4D/catalogues.json',
      recordCount: Array.isArray(catalogues) ? catalogues.length : 0,
      classification: 'NOT_VERIFIED',
      reason: 'Scraped third-party catalogue metadata; no written redistribution license verified in repo.'
    });
  }

  const shopsPath = path.join(repoRoot, 'D4D', 'shops.json');
  if (fs.existsSync(shopsPath)) {
    const shops = JSON.parse(fs.readFileSync(shopsPath, 'utf8'));
    entries.push({
      sourceName: 'D4D/shops.json',
      recordCount: Array.isArray(shops) ? shops.length : 0,
      classification: 'NOT_VERIFIED',
      reason: 'Third-party scraped retailer directory; branch coordinates absent/unverified.'
    });
  }

  const bundledFlyersDir = path.join(repoRoot, 'user-app', 'app', 'src', 'main', 'assets', 'flyers');
  if (fs.existsSync(bundledFlyersDir)) {
    const files = fs.readdirSync(bundledFlyersDir).filter((f) => /\.(jpg|jpeg|png|webp)$/i.test(f));
    entries.push({
      sourceName: 'user-app/app/src/main/assets/flyers',
      recordCount: files.length,
      classification: 'NOT_VERIFIED',
      reason: 'Bundled flyer assets derived from D4D; isolate to non-production demo unless merchant permission recorded.'
    });
  }

  return entries;
}

function main() {
  const __dirname = path.dirname(fileURLToPath(import.meta.url));
  const repoRoot = path.resolve(__dirname, '..');
  const report = evaluateSourceRights(repoRoot);
  console.log(JSON.stringify({ generatedAt: new Date().toISOString(), entries: report }, null, 2));
}

const isEntrypoint = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isEntrypoint) {
  main();
}
