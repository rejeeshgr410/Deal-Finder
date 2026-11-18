package com.example.domain.basket

import com.example.data.ProductEntity

enum class Objective {
    LOWEST_PRICE,
    FEWEST_STORES,
    PREFERRED_RETAILER
}

data class CanonicalUnit(
    val baseQuantity: Double,
    val baseUnit: String, // "g", "ml", "each"
    val packCount: Int = 1
)

data class ProductIdentity(
    val masterProductId: String = "",
    val barcodeGtin: String = "",
    val brand: String = "",
    val name: String = "",
    val variant: String = "",
    val baseQuantity: Double? = null,
    val baseUnit: String? = null,
    val packCount: Int = 1
)

data class RequestedItem(
    val lineId: String,
    val name: String,
    val identity: ProductIdentity,
    val requestedQuantity: Int,
    val referenceProductId: String = "",
    val allowEquivalents: Boolean = false
)

data class BasketRequest(
    val items: List<RequestedItem>,
    val selectedEmirate: String,
    val objective: Objective = Objective.LOWEST_PRICE,
    val maxStores: Int = 2,
    val preferredRetailerId: String? = null,
    val allowedStoreIds: Set<String>? = null,
    val hasUserLoyaltyCard: Boolean = false,
    val knownFeesByStoreFils: Map<String, Long> = emptyMap(),
    val nowMs: Long = System.currentTimeMillis(),
    val includeDemoOffers: Boolean = false
)

data class MatchedLine(
    val lineId: String,
    val itemName: String,
    val storeId: String,
    val storeName: String,
    val product: ProductEntity,
    val requestedQuantity: Int,
    val lineSubtotalFils: Long,
    val matchReason: String,
    val unitPriceText: String?,
    val verificationLabel: String,
    val availabilityDisclosure: String
)

data class UnpricedLine(
    val lineId: String,
    val itemName: String,
    val reason: String
)

data class BasketOption(
    val storeIds: List<String>,
    val storeNames: List<String>,
    val pricedLines: List<MatchedLine>,
    val unpricedLines: List<UnpricedLine>,
    val complete: Boolean,
    val itemCoverage: Int,
    val itemCount: Int,
    val verifiedItemsSubtotalFils: Long,
    val knownFeesFils: Long?,
    val totalFils: Long?,
    val unknownFeeDisclosure: String?,
    val comparisonDeltaVsBestSingleStoreFils: Long?,
    val savingsBaselineDisclosure: String,
    val sourceSnapshotAtMs: Long,
    val explanatoryWarnings: List<String>
)

data class BasketComparisonResult(
    val request: BasketRequest,
    val bestOneStore: BasketOption?,
    val bestTwoStore: BasketOption?,
    val preferredStoreOption: BasketOption?,
    val candidateStoreCount: Int,
    val storesTruncated: Boolean,
    val recommendationSummary: String,
    val coverageDisclosure: String
)
