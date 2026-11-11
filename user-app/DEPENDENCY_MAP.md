# Ecosystem Integration Audit: Dependency Map

## Deal Finder Business Action -> Firebase Collection -> Deal Finder Consumer Screen

This map demonstrates the fully synchronized real-time bindings between Business administration operations and the Consumer application endpoints.

| Business App Action      | Firebase Collection | Re-rendered Consumer Screens                        | Data Flow & Real-Time Listeners                            |
|--------------------------|----------------------|-----------------------------------------------------|-------------------------------------------------------------|
| **Create Product**       | `products`           | `HomeScreen`, `SearchScreen`, `ProductDetailScreen` | Synced via `addSnapshotListener` → `StateFlow<ProductEntity>`|
| **Edit Product**         | `products`           | `HomeScreen`, `SearchScreen`, `ProductDetailScreen` | Reactive UI automatically updates price / image             |
| **Delete Product**       | `products`           | `HomeScreen`, `SearchScreen`, `ProductDetailScreen` | Compose actively removes card from horizontal pagers        |
| **Upload Flyer**         | `flyers`             | None *(hidden until status is "approved")*         | Cached locally but gated computationally by status          |
| **Approve Flyer**        | `flyers`             | `HomeScreen`, `DealsScreen`, `FlyerViewerScreen`    | Passes `status == "approved"` filter → Pushed to client     |
| **Create Promotion**     | `promotions`         | `HomeScreen` (Active Promotions Row)               | Flows entirely through new `StateFlow<PromotionEntity>`     |
| **Create Coupon**        | `coupons`            | `CouponsScreen`                                     | Flow streams. Expires logically `expiry > System.currentTime`|

## Summary of Fixes

1. **Missing Synchronization for Promotions**: Previously, the `promotions` collection was entirely ignored in the Consumer app. This has been bound to the Repository via `asFlow()`, mapped dynamically to the `DealFinderViewModel`, and directly rendered on the `HomeScreen` via the new `PromotionCard` and `LazyRow`.
2. **Missing Synchronization for Coupons**: The `CouponsScreen` previously showed a static, non-functional layout. It now accepts the `DealFinderViewModel` and automatically maps coupons via snapshot listeners, automatically removing coupons that fail the condition `it.expiry > System.currentTimeMillis()`.
3. **Filter Synchronization**: Standardized the `approved` state in `DealFinderViewModel` to respect Business app moderations precisely using dynamic case-handling and bounding. All deleted elements in Business App successfully propagate through Firestore snapshots and actively remove the item locally from lists and screens avoiding exceptions.
