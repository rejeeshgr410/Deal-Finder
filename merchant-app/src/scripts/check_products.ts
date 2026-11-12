import { db } from '../firebase';
import { collection, getDocs } from 'firebase/firestore';

async function check() {
  const snap = await getDocs(collection(db, 'products'));
  console.log('Total products in Firestore:', snap.size);
  snap.docs.forEach(doc => {
    const d = doc.data();
    console.log(doc.id, '=>', d.name, '| retailer:', d.retailerId, '| flyerId:', d.flyerId, '| img:', d.imageUrl);
  });
}
check().then(() => process.exit(0)).catch(e => { console.error(e); process.exit(1); });
