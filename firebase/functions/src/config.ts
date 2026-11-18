import { getApps, initializeApp, App } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore, Firestore } from 'firebase-admin/firestore';

export const DEFAULT_NAMED_DATABASE_ID = 'ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096';

export function getAdminApp(): App {
  return getApps().length > 0 ? getApps()[0] : initializeApp();
}

export function getNamedDatabaseId(): string {
  const configured = (process.env.FIRESTORE_DATABASE_ID ?? DEFAULT_NAMED_DATABASE_ID).trim();
  if (!configured || configured === '(default)') {
    throw new Error('Refusing to use empty or (default) Firestore database ID; must target named database.');
  }
  return configured;
}

export function getNamedDb(): Firestore {
  const app = getAdminApp();
  return getFirestore(app, getNamedDatabaseId());
}

export function getAdminAuth() {
  return getAuth(getAdminApp());
}
