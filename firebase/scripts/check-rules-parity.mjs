import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '..', '..');

const pairs = [
  {
    name: 'firestore.rules',
    canonical: path.join(repoRoot, 'firebase', 'firestore.rules'),
    mirrors: [
      path.join(repoRoot, 'merchant-app', 'firestore.rules'),
      path.join(repoRoot, 'user-app', 'firestore.rules')
    ]
  },
  {
    name: 'storage.rules',
    canonical: path.join(repoRoot, 'firebase', 'storage.rules'),
    mirrors: [
      path.join(repoRoot, 'merchant-app', 'storage.rules'),
      path.join(repoRoot, 'user-app', 'storage.rules')
    ]
  }
];

let hasError = false;
for (const group of pairs) {
  const canonicalContent = fs.readFileSync(group.canonical, 'utf8').replace(/\r\n/g, '\n');
  for (const mirror of group.mirrors) {
    const mirrorContent = fs.readFileSync(mirror, 'utf8').replace(/\r\n/g, '\n');
    if (canonicalContent !== mirrorContent) {
      console.error(`PARITY MISMATCH for ${group.name}: ${mirror} differs from ${group.canonical}`);
      hasError = true;
    } else {
      console.log(`OK: ${path.relative(repoRoot, mirror)} matches ${path.relative(repoRoot, group.canonical)}`);
    }
  }
}

if (hasError) {
  process.exit(1);
}
