import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { mapV1ProductToV2Patch } from '../firebase/functions/src/contracts/offerV2';

const PRODUCTION_PROJECT_ID = 'gen-lang-client-0615495350';

export interface MigrationDryRunSummary {
  migrationRunId: string;
  dryRun: boolean;
  totalScanned: number;
  patchesPlanned: number;
  unverifiedCount: number;
  demoCount: number;
  checksumSha256: string;
  patches: Array<{ id: string; patch: Record<string, any> }>;
}

export function planV2ProductMigration(
  docs: Array<{ id: string; data: Record<string, any> }>,
  migrationRunId: string = `mig-v2-${Date.now()}`
): MigrationDryRunSummary {
  const patches = docs.map((entry) => ({
    id: entry.id,
    patch: mapV1ProductToV2Patch(entry.id, entry.data, migrationRunId)
  }));

  const unverifiedCount = patches.filter((p) => p.patch.rightsStatus === 'UNVERIFIED').length;
  const demoCount = patches.filter((p) => p.patch.isDemo === true).length;
  const checksumSha256 = crypto
    .createHash('sha256')
    .update(JSON.stringify(patches))
    .digest('hex');

  return {
    migrationRunId,
    dryRun: true,
    totalScanned: docs.length,
    patchesPlanned: patches.length,
    unverifiedCount,
    demoCount,
    checksumSha256,
    patches
  };
}

function main() {
  const args = process.argv.slice(2);
  const apply = args.includes('--apply');
  const allowProd = args.includes('--allow-production-migration');
  const projectIdx = args.indexOf('--confirm-project');
  const targetProject = projectIdx !== -1 ? args[projectIdx + 1] : 'demo-dealfinder-staging';
  const backupIdx = args.indexOf('--backup-file');
  const backupFile = backupIdx !== -1 ? args[backupIdx + 1] : '';
  const inputIdx = args.indexOf('--input');

  if (apply) {
    if (targetProject === PRODUCTION_PROJECT_ID && !allowProd) {
      console.error(`REFUSED: Production migration on "${PRODUCTION_PROJECT_ID}" requires --allow-production-migration and owner approval.`);
      process.exit(1);
    }
    if (!backupFile || !fs.existsSync(backupFile)) {
      console.error('REFUSED: --apply requires a verified --backup-file path on disk before any live write.');
      process.exit(1);
    }
  }

  let inputDocs: Array<{ id: string; data: Record<string, any> }> = [];
  if (inputIdx !== -1 && args[inputIdx + 1]) {
    const raw = JSON.parse(fs.readFileSync(path.resolve(process.cwd(), args[inputIdx + 1]), 'utf8'));
    const products = Array.isArray(raw.products) ? raw.products : [];
    inputDocs = products.map((p: any, idx: number) => ({ id: String(p.id || `doc-${idx}`), data: p }));
  }

  const summary = planV2ProductMigration(inputDocs);
  if (!apply) {
    console.log(
      JSON.stringify(
        {
          ...summary,
          patches: summary.patches.slice(0, 5) // Preview first 5 patches in dry-run stdout
        },
        null,
        2
      )
    );
    return;
  }

  console.error('Live Firestore batch apply is intentionally gated behind explicit Admin SDK credentials and staging verification.');
  process.exit(2);
}

const isEntrypoint = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isEntrypoint) {
  main();
}
