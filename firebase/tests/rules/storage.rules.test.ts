import { describe, it, beforeAll, afterAll } from 'vitest';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
  RulesTestEnvironment
} from '@firebase/rules-unit-testing';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { ref, uploadBytes, getBytes } from 'firebase/storage';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rulesPath = path.resolve(__dirname, '..', '..', 'storage.rules');

let testEnv: RulesTestEnvironment;

describe('Canonical Storage Security Rules', () => {
  beforeAll(async () => {
    const hostPort = process.env.FIREBASE_STORAGE_EMULATOR_HOST || '127.0.0.1:9199';
    const [host, portStr] = hostPort.split(':');
    testEnv = await initializeTestEnvironment({
      projectId: 'demo-dealfinder-staging',
      storage: {
        host,
        port: Number(portStr),
        rules: fs.readFileSync(rulesPath, 'utf8')
      }
    });
  });

  afterAll(async () => {
    await testEnv.cleanup();
  });

  it('blocks unauthenticated, consumer, and cross-tenant uploads while allowing authorized retailer uploads', async () => {
    const samplePdf = new Uint8Array([0x25, 0x50, 0x44, 0x46]); // %PDF
    const invalidText = new Uint8Array([0x41, 0x42, 0x43]);

    const anonStorage = testEnv.unauthenticatedContext().storage();
    await assertFails(
      uploadBytes(ref(anonStorage, 'flyers/retailer-a/test.pdf'), samplePdf, { contentType: 'application/pdf' })
    );

    const consumerStorage = testEnv.authenticatedContext('consumer-1', { role: 'consumer' }).storage();
    await assertFails(
      uploadBytes(ref(consumerStorage, 'flyers/retailer-a/test.pdf'), samplePdf, { contentType: 'application/pdf' })
    );

    const retailerBStorage = testEnv
      .authenticatedContext('owner-b', { role: 'retailer_owner', retailerId: 'retailer-b' })
      .storage();
    await assertFails(
      uploadBytes(ref(retailerBStorage, 'flyers/retailer-a/test.pdf'), samplePdf, { contentType: 'application/pdf' })
    );

    const retailerAStorage = testEnv
      .authenticatedContext('owner-a', { role: 'retailer_owner', retailerId: 'retailer-a' })
      .storage();

    // Invalid MIME denied even for authorized retailer
    await assertFails(
      uploadBytes(ref(retailerAStorage, 'flyers/retailer-a/bad.txt'), invalidText, { contentType: 'text/plain' })
    );

    // Valid PDF upload succeeds for owning retailer
    await assertSucceeds(
      uploadBytes(ref(retailerAStorage, 'flyers/retailer-a/valid.pdf'), samplePdf, { contentType: 'application/pdf' })
    );

    // Private extraction directory: owning retailer can read/write, public/consumer cannot read
    await assertSucceeds(
      uploadBytes(ref(retailerAStorage, 'private_extractions/retailer-a/raw.pdf'), samplePdf, {
        contentType: 'application/pdf'
      })
    );
    await assertFails(getBytes(ref(anonStorage, 'private_extractions/retailer-a/raw.pdf')));
    await assertFails(getBytes(ref(consumerStorage, 'private_extractions/retailer-a/raw.pdf')));
    await assertSucceeds(getBytes(ref(retailerAStorage, 'private_extractions/retailer-a/raw.pdf')));
  });
});
