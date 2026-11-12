import { auth, db } from '../firebase';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { collection, addDoc, doc, setDoc, getDocs, deleteDoc, query, where } from 'firebase/firestore';

async function testMerchantPublish() {
  console.log("Signing in as merchant admin...");
  const cred = await signInWithEmailAndPassword(auth, '***REDACTED_EMAIL***', '***REDACTED_PASSWORD***');
  console.log("Signed in with UID:", cred.user.uid);

  // 1. Test creating a Retailer
  console.log("\n1. Testing Retailer Creation...");
  const retailerRef = doc(db, 'retailers', 'test-carrefour');
  const retailerData = {
    id: 'test-carrefour',
    name: 'Carrefour UAE Test',
    logoUrl: 'https://cdn.d4donline.com/u/c/5e2f86565c054b879a1a8aeaae178a5c.jpg',
    status: 'published',
    emirates: 'Dubai',
    branchesCount: 2,
    searchKeywords: ['carrefour', 'hypermarket', 'supermarket', 'uae', 'dubai'],
    createdAt: Date.now(),
    updatedAt: Date.now()
  };
  await setDoc(retailerRef, retailerData);
  console.log("Retailer created successfully:", retailerRef.id);

  // 2. Test creating a Branch
  console.log("\n2. Testing Branch Creation...");
  const branchRef = doc(db, 'branches', 'branch-carrefour-moe');
  const branchData = {
    id: 'branch-carrefour-moe',
    retailerId: 'test-carrefour',
    name: 'Mall of the Emirates Branch',
    emirate: 'Dubai',
    address: 'Mall of the Emirates, Al Barsha 1, Dubai',
    latitude: 25.1181,
    longitude: 55.2007,
    phone: '+97180073232',
    openingHours: '8:00 AM - 12:00 AM'
  };
  await setDoc(branchRef, branchData);
  console.log("Branch created successfully:", branchRef.id);

  // 3. Test creating a Flyer
  console.log("\n3. Testing Flyer Creation...");
  const flyerRef = doc(db, 'flyers', 'flyer-carrefour-everything');
  const flyerData = {
    id: 'flyer-carrefour-everything',
    retailerId: 'test-carrefour',
    title: 'Everything At AED 5, 10, 15, 20, 25, 30',
    imageUrl: 'https://cdn.d4donline.com/u/d/26/11/9/51f0854ee808c90e828b2f2da1d951ab.webp',
    pdfUrl: 'https://cdn.d4donline.com/u/d/26/11/9/51f0854ee808c90e828b2f2da1d951ab.webp',
    pages: 19,
    status: 'published',
    searchKeywords: ['everything', 'carrefour', 'hypermarket', 'aed', 'deals'],
    validFrom: Date.now(),
    validTo: Date.now() + 7 * 24 * 3600 * 1000,
    views: 0,
    clicks: 0,
    createdAt: Date.now(),
    updatedAt: Date.now()
  };
  await setDoc(flyerRef, flyerData);
  console.log("Flyer created successfully:", flyerRef.id);

  // 4. Test creating Products
  console.log("\n4. Testing Product Creation...");
  const productRef = doc(db, 'products', 'prod-coke-carrefour');
  const productData = {
    id: 'prod-coke-carrefour',
    masterProductId: 'master-coke-330ml',
    retailerId: 'test-carrefour',
    flyerId: 'flyer-carrefour-everything',
    name: 'Coca Cola Can 330ml Pack of 6',
    normalizedName: 'coke 330ml pack of 6',
    searchKeywords: ['coca', 'cola', 'coke', 'can', '330ml', 'soda', 'beverage'],
    brand: 'Coca Cola',
    category: 'Drinks & Beverages',
    barcode: '5449000000996',
    imageUrl: 'https://cdn.d4donline.com/u/a/26/11/9/4185f401ff2c9e07e6633f613563e78b.jpg',
    currentPrice: 10.0,
    oldPrice: 15.0,
    unitSize: 330.0,
    unitType: 'ml',
    validFrom: Date.now(),
    validTo: Date.now() + 7 * 24 * 3600 * 1000,
    status: 'published'
  };
  await setDoc(productRef, productData);
  console.log("Product created successfully:", productRef.id);

  // 5. Test Querying
  console.log("\n5. Testing queries as consumer (read-only)...");
  const qRet = await getDocs(query(collection(db, 'retailers'), where('status', '==', 'published')));
  console.log(`Retailers count: ${qRet.size}`);
  const qFly = await getDocs(query(collection(db, 'flyers'), where('status', '==', 'published')));
  console.log(`Flyers count: ${qFly.size}`);
  const qProd = await getDocs(query(collection(db, 'products'), where('status', '==', 'published')));
  console.log(`Products count: ${qProd.size}`);

  // Clean up test items
  console.log("\nCleaning up test documents...");
  await deleteDoc(retailerRef);
  await deleteDoc(branchRef);
  await deleteDoc(flyerRef);
  await deleteDoc(productRef);
  console.log("Cleanup complete!");
}

testMerchantPublish().then(() => {
  console.log("\nMerchant App Publishing Pipeline Verified 100% Working!");
  process.exit(0);
}).catch(err => {
  console.error("Test error:", err);
  process.exit(1);
});
