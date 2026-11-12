import { db, auth } from '../firebase';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { collection, getDocs, doc, writeBatch } from 'firebase/firestore';

async function updateRealImages() {
  console.log("=== STEP 1: Authenticating ===");
  try {
    const cred = await signInWithEmailAndPassword(auth, '***REDACTED_EMAIL***', '***REDACTED_PASSWORD***');
    console.log("Authenticated as:", cred.user.email);
  } catch (e: any) {
    console.warn("Auth warning (proceeding):", e.message);
  }

  // 1. Update Products
  console.log("\n=== STEP 2: Updating Products ===");
  const prodSnap = await getDocs(collection(db, 'products'));
  console.log(`Found ${prodSnap.size} products to update.`);
  
  let batch = writeBatch(db);
  let count = 0;
  let batchNum = 0;

  for (const productDoc of prodSnap.docs) {
    const pId = productDoc.id;
    const realAssetUrl = `file:///android_asset/products/${pId}.jpg`;
    
    batch.update(doc(db, 'products', pId), {
      imageUrl: realAssetUrl,
      updatedAt: Date.now()
    });
    count++;

    if (count % 400 === 0) {
      await batch.commit();
      batch = writeBatch(db);
      batchNum++;
      console.log(`  Committed batch ${batchNum} (${count} products)...`);
    }
  }

  if (count % 400 !== 0) {
    await batch.commit();
    console.log(`  Committed final batch (${count} products).`);
  }

  // 2. Update Flyers
  console.log("\n=== STEP 3: Updating Flyers ===");
  const flyerSnap = await getDocs(collection(db, 'flyers'));
  console.log(`Found ${flyerSnap.size} flyers to update.`);

  batch = writeBatch(db);
  count = 0;
  for (const flyerDoc of flyerSnap.docs) {
    const fId = flyerDoc.id;
    const flyerAssetUrl = `file:///android_asset/flyers/${fId}.jpg`;

    batch.update(doc(db, 'flyers', fId), {
      imageUrl: flyerAssetUrl,
      updatedAt: Date.now()
    });
    count++;

    if (count % 400 === 0) {
      await batch.commit();
      batch = writeBatch(db);
      console.log(`  Committed flyers batch (${count} flyers)...`);
    }
  }
  if (count % 400 !== 0) {
    await batch.commit();
    console.log(`  Committed final flyers batch (${count} flyers).`);
  }

  // 3. Update Retailers
  console.log("\n=== STEP 4: Updating Retailers ===");
  const retSnap = await getDocs(collection(db, 'retailers'));
  console.log(`Found ${retSnap.size} retailers to update.`);

  batch = writeBatch(db);
  count = 0;
  for (const retDoc of retSnap.docs) {
    const rId = retDoc.id;
    const logoAssetUrl = `file:///android_asset/logos/${rId}.png`;

    batch.update(doc(db, 'retailers', rId), {
      logoUrl: logoAssetUrl,
      updatedAt: Date.now()
    });
    count++;
  }
  await batch.commit();
  console.log(`Committed all ${count} retailers.`);

  console.log("\n=======================================================");
  console.log("SUCCESS: All products, flyers, and retailers updated with real asset image URLs!");
  console.log("=======================================================");
}

updateRealImages().then(() => {
  console.log("Finished successfully.");
  process.exit(0);
}).catch(err => {
  console.error("Failed:", err);
  process.exit(1);
});
