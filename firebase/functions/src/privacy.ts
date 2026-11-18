import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { getNamedDb } from './config';

const USER_SUBCOLLECTIONS = [
  'savedDeals',
  'savedFlyers',
  'savedCoupons',
  'followedRetailers',
  'searchHistory',
  'alerts',
  'shoppingList',
] as const;

/**
 * S6.4 Account data deletion request:
 * Deletes the authenticated user's owned FCM tokens, user subcollections, and profile doc,
 * then writes a PII-free audit entry for compliance tracking.
 */
export const requestAccountDataDeletion = onCall({ cors: true }, async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Authentication required to delete account data.');
  }

  const uid = request.auth.uid;
  const db = getNamedDb();
  let deletedTokens = 0;
  let deletedSubdocs = 0;

  // 1. Delete all owned FCM tokens
  const tokensSnap = await db.collection('fcmTokens').where('userId', '==', uid).get();
  if (!tokensSnap.empty) {
    const batch = db.batch();
    tokensSnap.docs.forEach((doc) => {
      batch.delete(doc.ref);
      deletedTokens += 1;
    });
    await batch.commit();
  }

  // 2. Delete user-scoped subcollections
  const userRef = db.collection('users').doc(uid);
  for (const sub of USER_SUBCOLLECTIONS) {
    const subSnap = await userRef.collection(sub).get();
    if (!subSnap.empty) {
      const batch = db.batch();
      subSnap.docs.forEach((doc) => {
        batch.delete(doc.ref);
        deletedSubdocs += 1;
      });
      await batch.commit();
    }
  }

  // 3. Delete user profile document
  await userRef.delete();

  // 4. Write privacy-safe audit record (no email/PII)
  await db.collection('auditLogs').add({
    action: 'account_data_deleted',
    deletedTokens,
    deletedSubdocs,
    occurredAt: Date.now(),
  });

  return {
    ok: true,
    deletedTokens,
    deletedSubdocs,
  };
});
