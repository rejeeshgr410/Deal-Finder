package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await

class FirestoreDealFinderDao(private val db: FirebaseFirestore, private val sessionManager: SessionManager) : DealFinderDao {

    override fun getStores(): Flow<List<RetailerEntity>> {
        return db.collection("retailers")
            .whereEqualTo("status", "published")
            .limit(100)
            .asFlow()
    }

    override suspend fun getStoreById(storeId: String): RetailerEntity? {
        return try {
            val doc = db.collection("retailers").document(storeId).get().await()
            doc.toObject(RetailerEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun getFlyers(): Flow<List<FlyerEntity>> {
        val currentTime = System.currentTimeMillis()
        return db.collection("flyers")
            .whereEqualTo("status", "published")
            .limit(100)
            .asFlow()
    }

    override fun getProducts(): Flow<List<ProductEntity>> {
        return db.collection("products")
            .whereEqualTo("status", "published")
            .limit(100)
            .asFlow()
    }

    override fun searchProducts(query: String): Flow<List<ProductEntity>> {
        val words = query.lowercase().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return kotlinx.coroutines.flow.emptyFlow()
        val primaryKeyword = words.first()
        
        return db.collection("products")
            .whereEqualTo("status", "published")
            .whereArrayContains("searchKeywords", primaryKeyword)
            .limit(100)
            .asFlow()
    }

    override fun searchStores(query: String): Flow<List<RetailerEntity>> {
        val words = query.lowercase().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return kotlinx.coroutines.flow.emptyFlow()
        val primaryKeyword = words.first()
        
        return db.collection("retailers")
            .whereEqualTo("status", "published")
            .whereArrayContains("searchKeywords", primaryKeyword)
            .limit(100)
            .asFlow()
    }

    override fun searchFlyers(query: String): Flow<List<FlyerEntity>> {
        val words = query.lowercase().split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return kotlinx.coroutines.flow.emptyFlow()
        val primaryKeyword = words.first()
        
        return db.collection("flyers")
            .whereEqualTo("status", "published")
            .whereArrayContains("searchKeywords", primaryKeyword)
            .limit(100)
            .asFlow()
    }

    // Admin Operations
    override suspend fun updateUser(user: UserEntity) {
        try { db.collection("users").document(user.id).set(user, SetOptions.merge()).await() } catch (e: Exception) {}
    }

    override fun getCoupons(): Flow<List<CouponEntity>> {
        return db.collection("coupons")
            .whereEqualTo("status", "published")
            .limit(100)
            .asFlow()
    }

    override suspend fun insertUser(user: UserEntity) {
        try { db.collection("users").document(user.id).set(user, SetOptions.merge()).await() } catch (e: Exception) { }
    }

    override suspend fun getUserByEmail(email: String): UserEntity? {
        return try {
            val docs = db.collection("users").whereEqualTo("email", email).get().await()
            docs.documents.firstOrNull()?.toObject(UserEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun getUserByIdFlow(userId: String): Flow<UserEntity?> {
        return db.collection("users").document(userId).asDocumentFlow<UserEntity>()
    }

    override suspend fun getUserById(userId: String): UserEntity? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(UserEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveFcmToken(userId: String, token: String) {
        try {
            // Write to the TOP-LEVEL `fcmTokens` collection (keyed by the token), matching
            // what the business/admin app reads when sending push notifications
            // (it queries collection('fcmTokens') where status == 'active'). Writing to a
            // users/{uid}/fcmTokens subcollection instead would make consumer devices
            // invisible to the notification sender.
            db.collection("fcmTokens").document(token).set(mapOf(
                "id" to token,
                "userId" to userId,
                "status" to "active",
                "deviceType" to "Android",
                "updatedAt" to System.currentTimeMillis()
            )).await()
        } catch (e: Exception) {}
    }

    override fun getBranchesForStore(retailerId: String): Flow<List<BranchEntity>> {
        return db.collection("branches").whereEqualTo("retailerId", retailerId).asFlow()
    }

    override fun getAllBranches(): Flow<List<BranchEntity>> {
        return db.collection("branches").asFlow()
    }

    override fun getPromotions(): Flow<List<PromotionEntity>> {
        return db.collection("promotions")
            .whereEqualTo("status", "published")
            .limit(100)
            .asFlow()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getNotifications(): Flow<List<NotificationEntity>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId == null) kotlinx.coroutines.flow.emptyFlow()
            else db.collection("users").document(userId).collection("alerts")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100).asFlow()
        }
    }

    override suspend fun insertSearchHistory(history: SearchHistoryEntity) {
        val userId = sessionManager.getUserId() ?: return
        try { db.collection("users").document(userId).collection("searchHistory").document(history.query).set(history).await() } catch (e: Exception) {}
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getRecentSearches(): Flow<List<SearchHistoryEntity>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId == null) kotlinx.coroutines.flow.emptyFlow()
            else db.collection("users").document(userId).collection("searchHistory")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10).asFlow()
        }
    }

    override suspend fun deleteSearchHistory(query: String) {
        val userId = sessionManager.getUserId() ?: return
        try { db.collection("users").document(userId).collection("searchHistory").document(query).delete().await() } catch (e: Exception) {}
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getSavedDeals(): Flow<List<ProductEntity>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else db.collection("users").document(userId).collection("savedDeals").asFlow()
        }
    }

    override suspend fun toggleSavedDeal(product: ProductEntity) {
        val userId = sessionManager.getUserId() ?: return
        val docRef = db.collection("users").document(userId).collection("savedDeals").document(product.id)
        try {
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.delete().await()
            } else {
                docRef.set(product).await()
            }
        } catch (e: Exception) {}
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getSavedFlyers(): Flow<List<FlyerEntity>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else db.collection("users").document(userId).collection("savedFlyers").asFlow()
        }
    }

    override suspend fun toggleSavedFlyer(flyer: FlyerEntity) {
        val userId = sessionManager.getUserId() ?: return
        val docRef = db.collection("users").document(userId).collection("savedFlyers").document(flyer.id)
        try {
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.delete().await()
            } else {
                docRef.set(flyer).await()
            }
        } catch (e: Exception) {}
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getSavedCoupons(): Flow<List<CouponEntity>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else db.collection("users").document(userId).collection("savedCoupons").asFlow()
        }
    }

    override suspend fun toggleSavedCoupon(coupon: CouponEntity) {
        val userId = sessionManager.getUserId() ?: return
        val docRef = db.collection("users").document(userId).collection("savedCoupons").document(coupon.id)
        try {
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.delete().await()
            } else {
                docRef.set(coupon).await()
            }
        } catch (e: Exception) {}
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getFollowedRetailers(): Flow<List<RetailerEntity>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else db.collection("users").document(userId).collection("followedRetailers").asFlow()
        }
    }

    override suspend fun toggleFollowedRetailer(retailer: RetailerEntity) {
        val userId = sessionManager.getUserId() ?: return
        val docRef = db.collection("users").document(userId).collection("followedRetailers").document(retailer.id)
        try {
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.delete().await()
            } else {
                docRef.set(retailer).await()
            }
        } catch (e: Exception) {}
    }
}
