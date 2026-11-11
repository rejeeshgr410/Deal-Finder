package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "shopping_list")
data class ShoppingListItem(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val listId: String = "My Lists",
    val productId: String = "",
    val name: String = "",
    val targetPrice: Double = 0.0,
    val qty: Int = 1,
    val isSelected: Boolean = true
)

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list")
    fun getAllItems(): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItem)
    
    @Query("DELETE FROM shopping_list WHERE id = :id")
    suspend fun deleteItem(id: String)
    
    @Query("DELETE FROM shopping_list WHERE listId = :listId")
    suspend fun deleteList(listId: String)
    
    @Query("DELETE FROM shopping_list")
    suspend fun clear()
}

@Entity(tableName = "flyers")
data class CachedFlyerEntity(
    @PrimaryKey val id: String = "",
    val retailerId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val pdfUrl: String = "",
    val pages: Int = 0,
    val status: String = "published",
    val validFrom: Long = 0L,
    val validTo: Long = 0L,
    val views: Long = 0L,
    val clicks: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Dao
interface CachedFlyerDao {
    @Query("SELECT * FROM flyers")
    fun getAllFlyers(): Flow<List<CachedFlyerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlyers(flyers: List<CachedFlyerEntity>)
}

@Database(entities = [ShoppingListItem::class, CachedFlyerEntity::class], version = 2, exportSchema = false)
abstract class DealFinderRoomDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun cachedFlyerDao(): CachedFlyerDao
}

interface DealFinderDao {
    fun getStores(): Flow<List<RetailerEntity>>
    suspend fun getStoreById(storeId: String): RetailerEntity?

    fun getFlyers(): Flow<List<FlyerEntity>>

    fun getProducts(): Flow<List<ProductEntity>>
    fun searchProducts(query: String): Flow<List<ProductEntity>>
    fun searchStores(query: String): Flow<List<RetailerEntity>>
    fun searchFlyers(query: String): Flow<List<FlyerEntity>>

    // Admin Operations
    suspend fun updateUser(user: UserEntity)

    suspend fun insertUser(user: UserEntity)
    suspend fun getUserByEmail(email: String): UserEntity?
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>
    suspend fun getUserById(userId: String): UserEntity?

    suspend fun saveFcmToken(userId: String, token: String)

    fun getBranchesForStore(retailerId: String): Flow<List<BranchEntity>>
    fun getAllBranches(): Flow<List<BranchEntity>>

    fun getPromotions(): Flow<List<PromotionEntity>>

    fun getCoupons(): Flow<List<CouponEntity>>

    fun getNotifications(): Flow<List<NotificationEntity>>

    suspend fun insertSearchHistory(history: SearchHistoryEntity)
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>
    suspend fun deleteSearchHistory(query: String)

    fun getSavedDeals(): Flow<List<ProductEntity>>
    suspend fun toggleSavedDeal(product: ProductEntity)

    fun getSavedFlyers(): Flow<List<FlyerEntity>>
    suspend fun toggleSavedFlyer(flyer: FlyerEntity)

    fun getSavedCoupons(): Flow<List<CouponEntity>>
    suspend fun toggleSavedCoupon(coupon: CouponEntity)

    fun getFollowedRetailers(): Flow<List<RetailerEntity>>
    suspend fun toggleFollowedRetailer(retailer: RetailerEntity)
}
