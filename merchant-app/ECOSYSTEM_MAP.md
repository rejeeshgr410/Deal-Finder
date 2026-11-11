# Ecosystem Integration Map: Deal Finder UAE

## Overview
Deal Finder UAE operates as a dual-platform ecosystem interacting through a centralized real-time Firebase layer (Firestore, Auth, and Storage). Every data mutation performed within the **Business Application** is bound to Firestore `onSnapshot` listeners actively observed by the **Consumer Application**, guaranteeing zero-latency updates and immediate removal of expired content.

## Dependency Map: Business Action to Consumer Experience

### 1. Product Catalog Sync
*   **Business Action:** Create Product (via `Products` tab or OCR Bulk Publish)  
    *   **Firebase Action:** Writes document to `products` collection.  
    *   **Consumer Action:** Product immediately surfaces in global Search and Retailer-specific product feeds.
*   **Business Action:** Edit Product  
    *   **Firebase Action:** Updates fields (e.g., price, discount, valid dates) in `products/{id}`.  
    *   **Consumer Action:** Price modifications change instantly for the end consumer. Active Deals reflect the correct current pricing.
*   **Business Action:** Delete Product  
    *   **Firebase Action:** Deletes document `products/{id}`.  
    *   **Consumer Action:** Product proactively unmounts and vanishes from all active Consumer views (Search, Favorites, Trending).

### 2. Flyer & Promotion Distribution
*   **Business Action:** Upload Flyer  
    *   **Firebase Action:** Inserts document to `flyers` collection as `pending` (or `draft`) and uploads PDF to Storage `flyers/{retailerId}/{file}`.  
    *   **Consumer Action:** Remains completely hidden (due to Security Rules enforcing `status == 'published'`).
*   **Business Action:** Approve & Publish Flyer (OCR Workflow)  
    *   **Firebase Action:** Updates flyer `status` field to `published`. Extracts associated products to `products` collection.  
    *   **Consumer Action:** The new Flyer pops up on the Consumer Home feed ("New Flyers") and unlocks inside the Retailer's Page profile. Extracted products immediately populate global searches.
*   **Business Action:** Create Promotion/Coupon  
    *   **Firebase Action:** Writes document to `promotions` collection.  
    *   **Consumer Action:** Active Promotions instantly appear under the User's "Coupons" tab or the specific "Offers" lane if matched to a Retailer.

### 3. Expiration & Cleanup
*   **System Action:** Real-Time Expiration Filters  
    *   **Firebase Status:** Documents in `products` and `promotions` contain `date` and `expiry` parameters.  
    *   **Consumer Side:** The Consumer Client filters out all offers `where('expiryDate', '<', now)`.
*   **System Action:** Moderator Intervention  
    *   **Business Action:** Moderator deletes an expired flyer.  
    *   **Consumer Action:** Flyer disappears instantly from the mobile app's active browsing grid.

## Data Synchronization Strategy
*   **Stateless Hooks:** Both apps use pure React-driven Firestore Hooks built on top of `onSnapshot`. We intentionally bypass complex local state caching (like Redux middleware) in favor of allowing Firebase's native SDK persistence layer to provide single-source-of-truth real-time reactivity.
*   **Image Caching:** Media files (Flyer images) uploaded from the Business platform distribute via GCP Edge CDNs so that Consumer apps render heavy flyer multi-pages with zero perceptible latency.

## Remediation Applied During Audit
*   Deployed the full `Promotions & Coupons` management dashboard in the Business app to fulfill complete end-to-end promotion lifecycle management.
*   Built out structural `edit` capability within the Business App's Product grid so retailers can hot-patch price typos, which will instantly reflect to Consumers downstream.
