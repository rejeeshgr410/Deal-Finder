import { db } from '../firebase';
import { collection, getDocs, limit, query } from 'firebase/firestore';

async function checkCollections() {
  console.log("Checking Firestore collections in database:", db.app.options.projectId);
  
  const collections = ['retailers', 'flyers', 'products', 'branches', 'promotions', 'coupons'];
  
  for (const colName of collections) {
    try {
      const q = query(collection(db, colName), limit(5));
      const snap = await getDocs(q);
      console.log(`Collection '${colName}': ${snap.size} documents found (sampled)`);
      snap.forEach(doc => {
        console.log(`  [${doc.id}] =>`, JSON.stringify(doc.data()).slice(0, 150));
      });
    } catch (e: any) {
      console.error(`Collection '${colName}' error:`, e.message || e);
    }
  }
}

checkCollections().then(() => {
  console.log("Done checking collections.");
  process.exit(0);
}).catch(err => {
  console.error("Fatal error:", err);
  process.exit(1);
});
