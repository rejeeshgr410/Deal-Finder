import { describe, it, beforeAll, afterAll, beforeEach } from 'vitest';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
  RulesTestEnvironment
} from '@firebase/rules-unit-testing';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs, query, where } from 'firebase/firestore';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const rulesPath = path.resolve(__dirname, '..', '..', 'firestore.rules');
const NAMED_DB_ID = 'ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096';

let testEnv: RulesTestEnvironment;

describe('Canonical Firestore Security Rules (Named DB)', () => {
  beforeAll(async () => {
    const hostPort = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8080';
    const [host, portStr] = hostPort.split(':');
    testEnv = await initializeTestEnvironment({
      projectId: 'demo-dealfinder-staging',
      firestore: {
        host,
        port: Number(portStr),
        rules: fs.readFileSync(rulesPath, 'utf8')
      }
    });
  });

  afterAll(async () => {
    await testEnv.cleanup();
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      const db = ctx.firestore({ databaseId: NAMED_DB_ID } as any);
      await setDoc(doc(db, 'users', 'consumer-1'), {
        id: 'consumer-1',
        email: 'consumer1@example.com',
        role: 'consumer',
        isSuspended: false
      });
      await setDoc(doc(db, 'users', 'retailer-a-owner'), {
        id: 'retailer-a-owner',
        email: 'owner-a@example.com',
        role: 'retailer_owner',
        retailerId: 'retailer-a',
        isSuspended: false
      });
      await setDoc(doc(db, 'users', 'retailer-b-owner'), {
        id: 'retailer-b-owner',
        email: 'owner-b@example.com',
        role: 'retailer_owner',
        retailerId: 'retailer-b',
        isSuspended: false
      });
      await setDoc(doc(db, 'users', 'moderator-1'), {
        id: 'moderator-1',
        email: 'mod@example.com',
        role: 'moderator',
        isSuspended: false
      });
      await setDoc(doc(db, 'users', 'admin-1'), {
        id: 'admin-1',
        email: 'admin@example.com',
        role: 'admin',
        isSuspended: false
      });
      await setDoc(doc(db, 'retailers', 'retailer-a'), {
        id: 'retailer-a',
        name: 'Retailer A',
        status: 'published'
      });
      await setDoc(doc(db, 'flyers', 'flyer-pub-a'), {
        id: 'flyer-pub-a',
        retailerId: 'retailer-a',
        title: 'Published Flyer',
        status: 'published'
      });
      await setDoc(doc(db, 'flyers', 'flyer-draft-a'), {
        id: 'flyer-draft-a',
        retailerId: 'retailer-a',
        title: 'Draft Flyer',
        status: 'draft'
      });
      await setDoc(doc(db, 'products', 'prod-pub-a'), {
        id: 'prod-pub-a',
        retailerId: 'retailer-a',
        name: 'Milk 1L',
        currentPrice: 6.5,
        status: 'published'
      });
      await setDoc(doc(db, 'products', 'prod-draft-a'), {
        id: 'prod-draft-a',
        retailerId: 'retailer-a',
        name: 'Unpublished Item',
        currentPrice: 9.0,
        status: 'ocr_review'
      });
      await setDoc(doc(db, 'fcmTokens', 'tok-consumer-1'), {
        id: 'tok-consumer-1',
        userId: 'consumer-1',
        status: 'active',
        deviceType: 'Android',
        updatedAt: 1000
      });
      await setDoc(doc(db, 'notifications', 'note-1'), {
        title: 'Private Note',
        targetType: 'emirate',
        status: 'accepted_by_provider'
      });
      await setDoc(doc(db, 'analytics', 'evt-1'), {
        type: 'product_click',
        tenantId: 'retailer-a',
        timestamp: 1000
      });
      await setDoc(doc(db, 'auditLogs', 'log-1'), {
        collection: 'products',
        action: 'UPDATE',
        documentId: 'prod-pub-a',
        actorUid: 'moderator-1',
        timestamp: 1000
      });
    });
  });

  function dbFor(uid: string | null, claims?: Record<string, any>) {
    const ctx = uid
      ? testEnv.authenticatedContext(uid, claims)
      : testEnv.unauthenticatedContext();
    return ctx.firestore({ databaseId: NAMED_DB_ID } as any);
  }

  it('blocks anonymous reads of drafts, user docs, tokens, notifications, analytics, and auditLogs', async () => {
    const anonDb = dbFor(null);
    await assertSucceeds(getDoc(doc(anonDb, 'products', 'prod-pub-a')));
    await assertSucceeds(getDocs(query(collection(anonDb, 'products'), where('status', '==', 'published'))));
    await assertFails(getDoc(doc(anonDb, 'products', 'prod-draft-a')));
    await assertFails(getDoc(doc(anonDb, 'flyers', 'flyer-draft-a')));
    await assertFails(getDoc(doc(anonDb, 'users', 'consumer-1')));
    await assertFails(getDoc(doc(anonDb, 'fcmTokens', 'tok-consumer-1')));
    await assertFails(getDoc(doc(anonDb, 'notifications', 'note-1')));
    await assertFails(getDoc(doc(anonDb, 'analytics', 'evt-1')));
    await assertFails(getDoc(doc(anonDb, 'auditLogs', 'log-1')));
  });

  it('blocks consumer privilege escalation via self-create, self-update, or email spoofing', async () => {
    const newDb = dbFor('attacker-uid', { email: 'spoofed-admin@example.com' });
    await assertFails(
      setDoc(doc(newDb, 'users', 'attacker-uid'), {
        email: 'spoofed-admin@example.com',
        role: 'super_admin'
      })
    );
    await assertFails(
      setDoc(doc(newDb, 'users', 'attacker-uid'), {
        email: 'attacker@example.com',
        role: 'consumer',
        retailerId: 'retailer-a'
      })
    );
    await assertSucceeds(
      setDoc(doc(newDb, 'users', 'attacker-uid'), {
        email: 'attacker@example.com',
        role: 'consumer',
        isSuspended: false
      })
    );

    const consumerDb = dbFor('consumer-1');
    await assertFails(updateDoc(doc(consumerDb, 'users', 'consumer-1'), { role: 'admin' }));
    await assertFails(updateDoc(doc(consumerDb, 'users', 'consumer-1'), { retailerId: 'retailer-a' }));
    await assertFails(updateDoc(doc(consumerDb, 'users', 'consumer-1'), { isSuspended: true }));
    await assertSucceeds(updateDoc(doc(consumerDb, 'users', 'consumer-1'), { notifyPriceDrops: false }));
  });

  it('isolates private user subcollections and fcmTokens to owner', async () => {
    const consumer1 = dbFor('consumer-1');
    const consumer2 = dbFor('consumer-2');

    await assertSucceeds(
      setDoc(doc(consumer1, 'users', 'consumer-1', 'savedDeals', 'prod-pub-a'), { id: 'prod-pub-a' })
    );
    await assertFails(
      getDoc(doc(consumer2, 'users', 'consumer-1', 'savedDeals', 'prod-pub-a'))
    );
    await assertFails(
      setDoc(doc(consumer2, 'users', 'consumer-1', 'shoppingLists', 'My Lists'), { name: 'Hacked' })
    );

    await assertFails(getDoc(doc(consumer2, 'fcmTokens', 'tok-consumer-1')));
    await assertFails(
      setDoc(doc(consumer2, 'fcmTokens', 'tok-forged'), {
        id: 'tok-forged',
        userId: 'consumer-1',
        status: 'active',
        deviceType: 'Android',
        updatedAt: 1000
      })
    );
    await assertSucceeds(
      setDoc(doc(consumer2, 'fcmTokens', 'tok-consumer-2'), {
        id: 'tok-consumer-2',
        userId: 'consumer-2',
        status: 'active',
        deviceType: 'Android',
        updatedAt: 1000
      })
    );
  });

  it('enforces cross-tenant retailer isolation and blocks retailer self-publishing', async () => {
    const retailerA = dbFor('retailer-a-owner', { role: 'retailer_owner', retailerId: 'retailer-a' });
    const retailerB = dbFor('retailer-b-owner', { role: 'retailer_owner', retailerId: 'retailer-b' });
    const moderator = dbFor('moderator-1', { role: 'moderator' });

    // Retailer A cannot edit or read Retailer A's drafts from Retailer B context
    await assertFails(getDoc(doc(retailerB, 'flyers', 'flyer-draft-a')));
    await assertFails(
      setDoc(doc(retailerB, 'products', 'prod-forged'), {
        retailerId: 'retailer-a',
        name: 'Forged Deal',
        status: 'draft'
      })
    );
    await assertFails(updateDoc(doc(retailerB, 'products', 'prod-draft-a'), { currentPrice: 1.0 }));

    // Retailer A can create/update own drafts, but CANNOT self-publish globally
    await assertSucceeds(
      setDoc(doc(retailerA, 'flyers', 'flyer-new-a'), {
        retailerId: 'retailer-a',
        title: 'New Flyer',
        status: 'pending_admin_review'
      })
    );
    await assertFails(
      setDoc(doc(retailerA, 'flyers', 'flyer-self-pub'), {
        retailerId: 'retailer-a',
        title: 'Self Published',
        status: 'published'
      })
    );
    await assertFails(updateDoc(doc(retailerA, 'flyers', 'flyer-draft-a'), { status: 'published' }));

    // Moderator can publish
    await assertSucceeds(updateDoc(doc(moderator, 'flyers', 'flyer-draft-a'), { status: 'published' }));
  });

  it('blocks direct client writes to notifications, notificationDeliveries, analytics, and forged auditLogs', async () => {
    const consumer = dbFor('consumer-1');
    const admin = dbFor('admin-1', { role: 'admin' });

    await assertFails(setDoc(doc(consumer, 'notifications', 'spam'), { title: 'Spam', status: 'sent' }));
    await assertFails(setDoc(doc(admin, 'notifications', 'direct-note'), { title: 'Direct', status: 'sent' }));
    await assertFails(setDoc(doc(consumer, 'notificationDeliveries', 'del-1'), { status: 'sent' }));
    await assertFails(setDoc(doc(consumer, 'analytics', 'fake-evt'), { type: 'flyer_view' }));
    await assertFails(
      setDoc(doc(consumer, 'auditLogs', 'fake-log'), {
        collection: 'users',
        action: 'UPDATE',
        documentId: 'x',
        details: '{}',
        user: 'c@example.com',
        actorUid: 'consumer-1',
        timestamp: 1000
      })
    );
    await assertFails(deleteDoc(doc(admin, 'auditLogs', 'log-1')));

    // Authorized retailer staff writing their own actorUid audit log succeeds
    const retailerA = dbFor('retailer-a-owner', { role: 'retailer_owner', retailerId: 'retailer-a' });
    await assertSucceeds(
      setDoc(doc(retailerA, 'auditLogs', 'valid-log'), {
        collection: 'flyers',
        action: 'CREATE',
        documentId: 'flyer-new-a',
        details: '{}',
        user: 'owner-a@example.com',
        actorUid: 'retailer-a-owner',
        timestamp: 1000
      })
    );
  });

  it('prevents non-super_admin admin from creating or elevating super_admin', async () => {
    const admin = dbFor('admin-1', { role: 'admin' });
    await assertFails(updateDoc(doc(admin, 'users', 'consumer-1'), { role: 'super_admin' }));
    await assertSucceeds(updateDoc(doc(admin, 'users', 'consumer-1'), { role: 'moderator' }));
  });
});
