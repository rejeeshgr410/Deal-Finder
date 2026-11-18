import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export interface DemoIsolationResult {
  passed: boolean;
  violations: string[];
}

export function checkDemoIsolation(dataset: {
  products?: any[];
  flyers?: any[];
  branches?: any[];
  analytics?: any[];
}): DemoIsolationResult {
  const violations: string[] = [];

  for (const p of dataset.products ?? []) {
    if (p.isDemo === true || p.sourceType === 'demo' || String(p.imageUrl || '').startsWith('file:///android_asset/')) {
      violations.push(`PRODUCT_DEMO_IN_PROD:${p.id || p.name || 'unknown'}`);
    }
  }
  for (const f of dataset.flyers ?? []) {
    if (f.isDemo === true || f.sourceType === 'demo' || String(f.imageUrl || '').startsWith('file:///android_asset/')) {
      violations.push(`FLYER_DEMO_IN_PROD:${f.id || f.title || 'unknown'}`);
    }
  }
  for (const b of dataset.branches ?? []) {
    if (b.isDemo === true || b.coordSource === 'unverified_seed') {
      violations.push(`BRANCH_DEMO_IN_PROD:${b.id || b.name || 'unknown'}`);
    }
  }
  for (const a of dataset.analytics ?? []) {
    if (a.environment === 'demo' || a.environment === 'emulator' || a.isDemo === true) {
      violations.push(`ANALYTICS_DEMO_CONTAMINATION:${a.id || a.type || 'unknown'}`);
    }
  }

  return { passed: violations.length === 0, violations };
}

function main() {
  const args = process.argv.slice(2);
  const inputIdx = args.indexOf('--input');
  if (inputIdx === -1 || !args[inputIdx + 1]) {
    console.log(JSON.stringify({ passed: true, note: 'No --input snapshot supplied; helper ready.' }, null, 2));
    return;
  }
  const raw = JSON.parse(fs.readFileSync(path.resolve(process.cwd(), args[inputIdx + 1]), 'utf8'));
  const result = checkDemoIsolation(raw);
  console.log(JSON.stringify(result, null, 2));
  if (!result.passed) {
    process.exit(1);
  }
}

const isEntrypoint = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isEntrypoint) {
  main();
}
