package com.example.data

@com.google.firebase.firestore.IgnoreExtraProperties
data class UserEntity(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val passwordHash: String = "",
    val isGuest: Boolean = false,
    val role: String = "user",
    val isSuspended: Boolean = false,
    val notifyNewFlyers: Boolean = true,
    val notifyPriceDrops: Boolean = true,
    val notifyDealExpiry: Boolean = true,
    val notifyPromotions: Boolean = true,
    val notifyCouponExpiry: Boolean = true,
    val fcmToken: String = ""
)

data class RetailerEntity(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val status: String = "published",
    val emirates: String = "",
    val branchesCount: Int = 0,
    val searchKeywords: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class FlyerEntity(
    val id: String = "",
    val retailerId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val pdfUrl: String = "",
    val pages: Int = 0,
    val status: String = "published",
    val searchKeywords: List<String> = emptyList(),
    val validFrom: Long = 0L,
    val validTo: Long = 0L,
    val views: Long = 0L,
    val clicks: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@com.google.firebase.firestore.IgnoreExtraProperties
data class ProductEntity(
    val id: String = "",
    val masterProductId: String = "",
    val retailerId: String = "",
    val flyerId: String = "",
    val name: String = "",
    val normalizedName: String = "",
    val searchKeywords: List<String> = emptyList(),
    val brand: String = "",
    val category: String = "General",
    val barcode: String = "",
    val imageUrl: String = "",
    val currentPrice: Double = 0.0,
    val oldPrice: Double? = null,
    val unitSize: Double = 1.0,
    val unitType: String = "unit",
    val validFrom: Long = 0L,
    val validTo: Long = 0L,
    val status: String = "published",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    // Additive Schema V2 fields (nullable/defaulted for V1 backward compatibility)
    val schemaVersion: Int = 1,
    val amountMinor: Long? = null,
    val comparisonPriceMinor: Long? = null,
    val baseQuantity: Double? = null,
    val baseUnit: String? = null,
    val packCount: Int = 1,
    val minUnits: Int = 1,
    val buyUnits: Int? = null,
    val freeUnits: Int? = null,
    val loyaltyRequired: Boolean = false,
    val couponRequired: Boolean = false,
    val promoConditions: String = "",
    val availability: String = "unknown",
    val sourceType: String = "",
    val sourceLabel: String = "",
    val verificationLabel: String = "unverified",
    val lastVerifiedAt: Long? = null,
    val isDemo: Boolean = false
)

fun ProductEntity.isIdenticalTo(other: ProductEntity): Boolean {
    return com.example.domain.basket.ProductIdentityMatcher.areExactSameSku(this, other)
}

fun ProductEntity.matchesSearchName(otherName: String): Boolean {
    val n1 = this.name.normalizeProduct()
    val n2 = otherName.normalizeProduct()
    if (n1 == n2 || n1.contains(n2) || n2.contains(n1)) return true
    
    val tokens1 = n1.split(" ").filter { it.isNotBlank() && it.length > 2 }.toSet()
    val tokens2 = n2.split(" ").filter { it.isNotBlank() && it.length > 2 }.toSet()
    if (tokens1.isNotEmpty() && tokens2.isNotEmpty()) {
        val intersection = tokens1.intersect(tokens2)
        if (intersection.size >= Math.max(1, (Math.min(tokens1.size, tokens2.size) * 0.7).toInt())) {
            return true
        }
    }
    return false
}

private fun String.normalizeProduct(): String {
    return this.lowercase()
        .replace(Regex("coca[- ]cola"), "coke")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

data class PriceEntity(
    val id: Int = 0,
    val productId: String = "",
    val retailerId: String = "",
    val price: Double = 0.0,
    val timestamp: Long = 0L
)

data class AlertEntity(
    val id: Int = 0,
    val productId: String = "",
    val targetPrice: Double = 0.0,
    val isActive: Boolean = false
)

data class DealEntity(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val expiry: Long = 0L
)

@com.google.firebase.firestore.IgnoreExtraProperties
data class CouponEntity(
    val id: String = "",
    val retailerId: String = "",
    val title: String = "",
    val code: String = "",
    val discountInfo: String = "",
    val expiry: Long = 0L,
    val status: String = "published",
    val storeName: String = "",
    val domain: String = "",
    val category: String = "",
    val description: String = "",
    val isGlobal: Boolean = false,
    val isFeatured: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@com.google.firebase.firestore.IgnoreExtraProperties
data class BranchEntity(
    val id: String = "",
    val retailerId: String = "",
    val name: String = "",
    val emirate: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String = "",
    val openingHours: String = "",
    val coordsVerified: Boolean = false,
    val coordSource: String = ""
)

data class PromotionEntity(
    val id: String = "",
    val retailerId: String = "",
    val title: String = "",
    val description: String = "",
    val discount: Double = 0.0,
    val validFrom: Long = 0L,
    val validTo: Long = 0L,
    val status: String = "published",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class SearchHistoryEntity(
    val query: String = "",
    val timestamp: Long = 0L
)

data class NotificationEntity(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

@androidx.room.Entity(tableName = "loyalty_cards")
data class LoyaltyCardEntity(
    @androidx.room.PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val storeName: String = "",
    val cardNumber: String = "",
    val barcodeFormat: String = "CODE_128", // CODE_128, EAN_13, QR_CODE
    val colorHex: Long = 0xFF004F9F,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


