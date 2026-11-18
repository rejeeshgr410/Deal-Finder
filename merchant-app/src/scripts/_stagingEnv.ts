import 'dotenv/config';
import firebaseConfig from '../../firebase-applet-config.json';

const PRODUCTION_PROJECT_ID = 'gen-lang-client-0615495350';

export function assertStagingTarget(scriptName: string): void {
  const isEmulator = Boolean(process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_AUTH_EMULATOR_HOST);
  if (
    !isEmulator &&
    firebaseConfig.projectId === PRODUCTION_PROJECT_ID &&
    process.env.ALLOW_NON_STAGING_SCRIPT !== 'true'
  ) {
    throw new Error(
      `[${scriptName}] Refusing to run against production project "${PRODUCTION_PROJECT_ID}". ` +
        'Configure a staging Firebase config or Firebase Emulator Suite before running.'
    );
  }
}

export function getStagingCredentials(): { email: string; password: string } {
  const email = process.env.STAGING_ADMIN_EMAIL?.trim() || '';
  const password = process.env.STAGING_ADMIN_PASSWORD?.trim() || '';
  if (!email || !password) {
    throw new Error(
      'Missing STAGING_ADMIN_EMAIL or STAGING_ADMIN_PASSWORD environment variables. ' +
        'Do not hardcode credentials in tracked scripts.'
    );
  }
  return { email, password };
}
