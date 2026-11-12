import { auth, db } from '../firebase';
import { createUserWithEmailAndPassword, signInWithEmailAndPassword } from 'firebase/auth';
import { doc, setDoc } from 'firebase/firestore';

async function main() {
  const email = '***REDACTED_EMAIL***';
  const pass = '***REDACTED_PASSWORD***';
  let uid: string;
  try {
    const cred = await createUserWithEmailAndPassword(auth, email, pass);
    uid = cred.user.uid;
    console.log('Created user:', uid);
  } catch (e: any) {
    if (e.code === 'auth/email-already-in-use') {
      const cred = await signInWithEmailAndPassword(auth, email, pass);
      uid = cred.user.uid;
      console.log('Signed in existing user:', uid);
    } else {
      console.error('Auth error:', e.code, e.message);
      return;
    }
  }

  // Set user doc
  try {
    await setDoc(doc(db, 'users', uid), {
      email,
      role: 'super_admin',
      displayName: 'Merchant Admin',
      createdAt: Date.now(),
      updatedAt: Date.now()
    }, { merge: true });
    console.log('Updated user profile in firestore!');
  } catch (err: any) {
    console.error('Firestore user set error:', err.code, err.message);
  }
}

main().then(() => {
  console.log('Done.');
  process.exit(0);
}).catch(err => {
  console.error(err);
  process.exit(1);
});
