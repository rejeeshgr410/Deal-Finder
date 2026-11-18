import { auth, db } from '../firebase';
import { signInAnonymously, createUserWithEmailAndPassword, signInWithEmailAndPassword } from 'firebase/auth';
import { collection, addDoc, doc, setDoc, getDocs } from 'firebase/firestore';
import { assertStagingTarget, getStagingCredentials } from './_stagingEnv';

async function testAuth() {
  assertStagingTarget('test_auth_and_write');
  const { email, password: testPass } = getStagingCredentials();
  console.log("Testing Firebase Auth (Staging)...");

  let userCredential;
  try {
    console.log(`Attempting login with ${email}...`);
    userCredential = await signInWithEmailAndPassword(auth, email, testPass);
    console.log("Successfully logged in with email/pass:", userCredential.user.uid);
  } catch (err: any) {
    console.log("Login failed:", err.code, err.message);
    try {
      console.log(`Attempting create user ${email}...`);
      userCredential = await createUserWithEmailAndPassword(auth, email, testPass);
      console.log("Created user successfully:", userCredential.user.uid);
    } catch (createErr: any) {
      console.log("Create user failed:", createErr.code, createErr.message);
      try {
        console.log("Attempting anonymous sign in...");
        userCredential = await signInAnonymously(auth);
        console.log("Anonymous sign in succeeded:", userCredential.user.uid);
      } catch (anonErr: any) {
        console.log("Anonymous sign in failed:", anonErr.code, anonErr.message);
      }
    }
  }

  if (auth.currentUser) {
    console.log("Current user:", auth.currentUser.email, auth.currentUser.uid);
    // Try writing to retailers
    try {
      const testRetailer = {
        name: "Test Store",
        status: "published",
        emirates: "Dubai",
        branchesCount: 1,
        createdAt: Date.now(),
        updatedAt: Date.now()
      };
      const res = await addDoc(collection(db, "retailers"), testRetailer);
      console.log("Successfully wrote test retailer:", res.id);
    } catch (writeErr: any) {
      console.error("Failed to write to retailers:", writeErr.code, writeErr.message);
    }
  }
}

testAuth().then(() => process.exit(0)).catch(e => { console.error(e); process.exit(1); });
