import fs from 'fs';
import path from 'path';
import { auth, db } from '../firebase';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { doc, writeBatch } from 'firebase/firestore';
import { assertStagingTarget, getStagingCredentials } from './_stagingEnv';

const D4D_DIR = 'C:/Reji/Android Apps/Deal Finder/D4D';

async function seedData() {
  assertStagingTarget('seed_d4d_data');
  const { email, password } = getStagingCredentials();
  console.log("=== STEP 1: Authenticating as Staging Admin ===");
  const cred = await signInWithEmailAndPassword(auth, email, password);
  console.log("Authenticated as:", cred.user.email, cred.user.uid);

  // Read shops.json
  const shopsRaw = fs.readFileSync(path.join(D4D_DIR, 'shops.json'), 'utf-8');
  const shops = JSON.parse(shopsRaw);
  console.log(`Loaded ${shops.length} shops from D4D`);

  // Read catalogues.json
  const catsRaw = fs.readFileSync(path.join(D4D_DIR, 'catalogues.json'), 'utf-8');
  const catalogues = JSON.parse(catsRaw);
  console.log(`Loaded ${catalogues.length} catalogues from D4D`);

  // Helper to commit batches
  let batch = writeBatch(db);
  let opCount = 0;
  const commitBatchIfNeeded = async (force = false) => {
    if (opCount >= 400 || (force && opCount > 0)) {
      console.log(`  -> Committing batch with ${opCount} operations...`);
      await batch.commit();
      batch = writeBatch(db);
      opCount = 0;
    }
  };

  console.log("\n=== STEP 2: Seeding Retailers & Branches ===");
  const retailerIds = new Set<string>();

  for (const shop of shops) {
    const retailerRef = doc(db, 'retailers', shop.id);
    const keywords = [
      ...shop.name.toLowerCase().split(/\s+/),
      shop.id.toLowerCase(),
      'dubai',
      'uae',
      'supermarket',
      'hypermarket',
      'offers',
      'deals'
    ].filter(Boolean);

    const retailerData = {
      id: shop.id,
      name: shop.name,
      logoUrl: shop.logo_url || 'https://cdn.d4donline.com/img/icons/medium/icon.png',
      status: 'published',
      emirates: 'Dubai, UAE',
      branchesCount: Math.max(1, shop.branches?.length || 1),
      searchKeywords: Array.from(new Set(keywords)),
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    batch.set(retailerRef, retailerData);
    opCount++;
    retailerIds.add(shop.id);

    // Seed branches
    const branchNames = (shop.branches && shop.branches.length > 0) ? shop.branches : [`${shop.name} - Dubai Main Branch`];
    for (let bIdx = 0; bIdx < branchNames.length; bIdx++) {
      const bName = branchNames[bIdx];
      const branchId = `branch-${shop.id}-${bIdx + 1}`;
      const branchRef = doc(db, 'branches', branchId);
      
      const branchData = {
        id: branchId,
        retailerId: shop.id,
        name: bName.includes('Branch') ? bName : `${bName} Branch`,
        emirate: 'Dubai',
        address: `${bName}, Dubai, United Arab Emirates`,
        latitude: 25.10 + (Math.random() * 0.15),
        longitude: 55.15 + (Math.random() * 0.20),
        phone: shop.phone || '+971 4 000 0000',
        openingHours: '8:00 AM - 11:30 PM'
      };

      batch.set(branchRef, branchData);
      opCount++;
      await commitBatchIfNeeded();
    }
  }

  console.log("\n=== STEP 3: Seeding Flyers & Catalogues ===");
  for (const cat of catalogues) {
    const flyerId = String(cat.flyer_id);
    const flyerRef = doc(db, 'flyers', flyerId);

    const titleKeywords = [
      ...cat.title.toLowerCase().split(/[\s,–-]+/),
      ...cat.shop_name.toLowerCase().split(/\s+/),
      'flyer',
      'catalogue',
      'offers'
    ].filter(k => k.length > 2);

    const validFrom = cat.date_published ? Date.parse(cat.date_published) : Date.now();
    const validTo = cat.expiry_date ? Date.parse(cat.expiry_date) : (Date.now() + 10 * 86400000);

    const flyerData = {
      id: flyerId,
      retailerId: cat.shop_id,
      title: cat.title,
      imageUrl: `file:///android_asset/flyers/${flyerId}.jpg`,
      pdfUrl: cat.pdf_path || '',
      pages: cat.pages_count || 4,
      status: 'published',
      searchKeywords: Array.from(new Set(titleKeywords)),
      validFrom: isNaN(validFrom) ? Date.now() : validFrom,
      validTo: isNaN(validTo) ? (Date.now() + 10 * 86400000) : validTo,
      views: Math.floor(100 + Math.random() * 400),
      clicks: Math.floor(15 + Math.random() * 80),
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    batch.set(flyerRef, flyerData);
    opCount++;
    await commitBatchIfNeeded();
  }

  console.log("\n=== STEP 4: Seeding Multi-Store Comparison Products ===");
  const masterProducts = [
    {
      id: 'master-coke-6x330ml',
      name: 'Coca Cola Can 330ml (Pack of 6)',
      normalizedName: 'coke 330ml pack of 6',
      brand: 'Coca Cola',
      category: 'Drinks & Beverages',
      barcode: '5449000000996',
      unitSize: 330.0,
      unitType: 'ml',
      imageUrl: 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 11.50, old: 15.00 },
        'lulu-hypermarket-267': { current: 10.90, old: 14.50 },
        'nesto-hypermarket-284': { current: 9.99, old: 13.50 },
        'west-zone-supermarket-456': { current: 11.00, old: 14.00 },
        'safari-hypermarket-3883': { current: 10.50, old: 13.90 },
        'grand-hyper-market-440': { current: 10.25, old: 14.00 }
      }
    },
    {
      id: 'master-almarai-milk-2l',
      name: 'Almarai Fresh Milk Full Cream 2L',
      normalizedName: 'almarai fresh milk full cream 2l',
      brand: 'Almarai',
      category: 'Dairy & Eggs',
      barcode: '6281007010045',
      unitSize: 2.0,
      unitType: 'L',
      imageUrl: 'https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 11.00, old: 12.50 },
        'lulu-hypermarket-267': { current: 10.50, old: 12.00 },
        'nesto-hypermarket-284': { current: 9.75, old: 11.50 },
        'west-zone-supermarket-456': { current: 10.50, old: 12.00 },
        'almaya-supermarkets-271': { current: 11.25, old: 12.50 },
        'safeer-market-313': { current: 10.25, old: 12.00 }
      }
    },
    {
      id: 'master-alrawdah-chicken-1kg',
      name: 'Al Rawdah Fresh Chicken 1000g',
      normalizedName: 'al rawdah fresh chicken 1000g',
      brand: 'Al Rawdah',
      category: 'Chicken, Meat & Fish',
      barcode: '6291001000101',
      unitSize: 1000.0,
      unitType: 'g',
      imageUrl: 'https://images.unsplash.com/photo-1587593810167-a84920ea0781?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 16.50, old: 20.00 },
        'lulu-hypermarket-267': { current: 15.75, old: 19.50 },
        'nesto-hypermarket-284': { current: 14.50, old: 18.00 },
        'west-zone-supermarket-456': { current: 15.25, old: 19.00 },
        'grand-hyper-market-440': { current: 14.90, old: 18.50 }
      }
    },
    {
      id: 'master-sunwhite-rice-5kg',
      name: 'Sunwhite Calrose Rice 5kg',
      normalizedName: 'sunwhite calrose rice 5kg',
      brand: 'Sunwhite',
      category: 'Food - Grocery',
      barcode: '9310155001052',
      unitSize: 5.0,
      unitType: 'kg',
      imageUrl: 'https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 32.50, old: 38.00 },
        'lulu-hypermarket-267': { current: 29.90, old: 36.50 },
        'nesto-hypermarket-284': { current: 27.99, old: 35.00 },
        'safari-hypermarket-3883': { current: 28.50, old: 36.00 },
        'grand-hyper-market-440': { current: 28.25, old: 35.50 }
      }
    },
    {
      id: 'master-tide-powder-5kg',
      name: 'Tide Automatic Washing Powder 5kg',
      normalizedName: 'tide automatic washing powder 5kg',
      brand: 'Tide',
      category: 'Laundry & Cleaning',
      barcode: '8001090123456',
      unitSize: 5.0,
      unitType: 'kg',
      imageUrl: 'https://images.unsplash.com/photo-1610557892470-55d9e80c0bce?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 45.00, old: 58.00 },
        'lulu-hypermarket-267': { current: 42.50, old: 56.00 },
        'nesto-hypermarket-284': { current: 39.99, old: 54.00 },
        'west-zone-supermarket-456': { current: 43.00, old: 57.00 },
        'safari-hypermarket-3883': { current: 41.00, old: 55.00 }
      }
    },
    {
      id: 'master-nutella-750g',
      name: 'Nutella Hazelnut Cocoa Spread 750g',
      normalizedName: 'nutella hazelnut cocoa spread 750g',
      brand: 'Nutella',
      category: 'Food - Grocery',
      barcode: '8000500179864',
      unitSize: 750.0,
      unitType: 'g',
      imageUrl: 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 26.00, old: 32.00 },
        'lulu-hypermarket-267': { current: 24.50, old: 31.00 },
        'nesto-hypermarket-284': { current: 22.99, old: 29.50 },
        'west-zone-supermarket-456': { current: 25.00, old: 31.50 },
        'almaya-supermarkets-271': { current: 25.50, old: 32.00 }
      }
    },
    {
      id: 'master-dettol-liquid-2x1l',
      name: 'Dettol Antiseptic Disinfectant Liquid 2 x 1L',
      normalizedName: 'dettol antiseptic disinfectant liquid 2 x 1l',
      brand: 'Dettol',
      category: 'Health & Beauty',
      barcode: '5000158068742',
      unitSize: 2.0,
      unitType: 'L',
      imageUrl: 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 38.00, old: 48.00 },
        'lulu-hypermarket-267': { current: 34.90, old: 46.00 },
        'nesto-hypermarket-284': { current: 31.99, old: 44.00 },
        'safeer-market-313': { current: 33.50, old: 45.00 },
        'day-to-day-department-store-620': { current: 32.50, old: 44.00 }
      }
    },
    {
      id: 'master-lipton-tea-100s',
      name: 'Lipton Yellow Label Black Tea Bags 100s',
      normalizedName: 'lipton yellow label black tea bags 100s',
      brand: 'Lipton',
      category: 'Food - Grocery',
      barcode: '6281006000016',
      unitSize: 100.0,
      unitType: 'unit',
      imageUrl: 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 14.50, old: 18.00 },
        'lulu-hypermarket-267': { current: 13.75, old: 17.50 },
        'nesto-hypermarket-284': { current: 12.50, old: 16.00 },
        'day-to-day-department-store-620': { current: 12.99, old: 16.50 },
        'west-zone-supermarket-456': { current: 14.00, old: 17.50 }
      }
    },
    {
      id: 'master-fresh-eggs-30s',
      name: 'Fresh Farm Eggs Medium 30 Pack',
      normalizedName: 'fresh farm eggs medium 30 pack',
      brand: 'Al Jazira',
      category: 'Dairy & Eggs',
      barcode: '6291003000030',
      unitSize: 30.0,
      unitType: 'unit',
      imageUrl: 'https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 16.50, old: 19.00 },
        'lulu-hypermarket-267': { current: 15.25, old: 18.50 },
        'nesto-hypermarket-284': { current: 13.99, old: 17.00 },
        'west-zone-supermarket-456': { current: 15.00, old: 18.00 },
        'grand-hyper-market-440': { current: 14.50, old: 17.50 }
      }
    },
    {
      id: 'master-iphone-15-128gb',
      name: 'Apple iPhone 15 128GB Black',
      normalizedName: 'apple iphone 15 128gb black',
      brand: 'Apple',
      category: 'Electronics',
      barcode: '195949038234',
      unitSize: 1.0,
      unitType: 'unit',
      imageUrl: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 2949.00, old: 3199.00 },
        'lulu-hypermarket-267': { current: 2899.00, old: 3199.00 },
        'nesto-hypermarket-284': { current: 2879.00, old: 3149.00 },
        'safari-hypermarket-3883': { current: 2849.00, old: 3149.00 }
      }
    },
    {
      id: 'master-samsung-tv-55-4k',
      name: 'Samsung 55 Inch 4K UHD Smart TV Crystal',
      normalizedName: 'samsung 55 inch 4k uhd smart tv crystal',
      brand: 'Samsung',
      category: 'Electronics',
      barcode: '8806091234567',
      unitSize: 1.0,
      unitType: 'unit',
      imageUrl: 'https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 1549.00, old: 1999.00 },
        'lulu-hypermarket-267': { current: 1499.00, old: 1949.00 },
        'nesto-hypermarket-284': { current: 1449.00, old: 1899.00 },
        'safari-hypermarket-3883': { current: 1399.00, old: 1849.00 }
      }
    },
    {
      id: 'master-noor-oil-15l',
      name: 'Noor Pure Sunflower Oil 2 x 1.5L',
      normalizedName: 'noor pure sunflower oil 2 x 1 5l',
      brand: 'Noor',
      category: 'Food - Grocery',
      barcode: '6291004000150',
      unitSize: 3.0,
      unitType: 'L',
      imageUrl: 'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=400&q=80',
      prices: {
        'carrefour-uae-259': { current: 29.50, old: 36.00 },
        'lulu-hypermarket-267': { current: 26.90, old: 34.50 },
        'nesto-hypermarket-284': { current: 24.99, old: 33.00 },
        'west-zone-supermarket-456': { current: 27.50, old: 35.00 },
        'grand-hyper-market-440': { current: 25.50, old: 33.50 }
      }
    }
  ];

  for (const master of masterProducts) {
    const keywords = [
      ...master.name.toLowerCase().split(/[\s,–-]+/),
      ...master.brand.toLowerCase().split(/\s+/),
      ...master.category.toLowerCase().split(/[\s,–-]+/),
      master.barcode
    ].filter(k => k.length >= 2);

    for (const [rId, priceInfo] of Object.entries(master.prices)) {
      if (!retailerIds.has(rId)) continue;

      const prodId = `${master.id}-${rId}`;
      const prodRef = doc(db, 'products', prodId);

      const prodData = {
        id: prodId,
        masterProductId: master.id,
        retailerId: rId,
        flyerId: '',
        name: master.name,
        normalizedName: master.normalizedName,
        searchKeywords: Array.from(new Set(keywords)),
        brand: master.brand,
        category: master.category,
        barcode: master.barcode,
        imageUrl: `file:///android_asset/products/${prodId}.jpg`,
        currentPrice: priceInfo.current,
        oldPrice: priceInfo.old,
        unitSize: master.unitSize,
        unitType: master.unitType,
        validFrom: Date.now() - 2 * 86400000,
        validTo: Date.now() + 14 * 86400000,
        status: 'published'
      };

      batch.set(prodRef, prodData);
      opCount++;
      await commitBatchIfNeeded();
    }
  }

  console.log("\n=== STEP 5: Seeding Promotional Coupons ===");
  const coupons = [
    {
      id: 'coupon-carrefour-20',
      retailerId: 'carrefour-uae-259',
      title: 'AED 20 OFF on orders above AED 150',
      code: 'CARREFOUR20',
      discountInfo: 'AED 20 OFF',
      expiry: Date.now() + 14 * 86400000,
      status: 'published'
    },
    {
      id: 'coupon-lulu-15',
      retailerId: 'lulu-hypermarket-267',
      title: '15% OFF Electronics & Tech Deals',
      code: 'LULUTECH15',
      discountInfo: '15% OFF',
      expiry: Date.now() + 10 * 86400000,
      status: 'published'
    },
    {
      id: 'coupon-nesto-10',
      retailerId: 'nesto-hypermarket-284',
      title: '10% Cashback on Weekend Grocery Basket',
      code: 'NESTOWEEKEND',
      discountInfo: '10% Cashback',
      expiry: Date.now() + 7 * 86400000,
      status: 'published'
    }
  ];

  for (const coup of coupons) {
    const coupRef = doc(db, 'coupons', coup.id);
    batch.set(coupRef, coup);
    opCount++;
    await commitBatchIfNeeded();
  }

  // Final commit
  await commitBatchIfNeeded(true);
  console.log("\n=======================================================");
  console.log("SUCCESS: All Retailers, Branches, Catalogues, Products & Coupons Uploaded!");
  console.log("=======================================================");
}

seedData().then(() => {
  console.log("Seed script completed successfully.");
  process.exit(0);
}).catch(err => {
  console.error("Seed script failed:", err);
  process.exit(1);
});
