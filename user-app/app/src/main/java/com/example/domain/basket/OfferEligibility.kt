package com.example.domain.basket

import com.example.data.ProductEntity
import com.example.data.effectiveAmountMinor
import com.example.data.isCurrentlyEligible

data class PricedOfferEvaluation(
    val product: ProductEntity,
    val lineSubtotalFils: Long,
    val unitPriceText: String?,
    val matchReason: String
)

object OfferEligibility {

    fun isProductEligibleForRequest(
        product: ProductEntity,
        requestedQuantity: Int,
        request: BasketRequest
    ): Boolean {
        if (requestedQuantity <= 0) return false
        if (!product.isCurrentlyEligible(nowMs = request.nowMs, includeDemo = request.includeDemoOffers)) {
            return false
        }
        if (request.allowedStoreIds != null && product.retailerId !in request.allowedStoreIds) {
            return false
        }
        if (product.loyaltyRequired && !request.hasUserLoyaltyCard) {
            return false
        }
        if (product.minUnits > 1 && requestedQuantity < product.minUnits) {
            return false
        }
        return true
    }

    /**
     * Computes deterministic line subtotal in integer fils, respecting BOGO/multibuy conditions
     * without dividing bundle deals down to invalid single-unit discounts.
     */
    fun computeLineSubtotalFils(product: ProductEntity, requestedQuantity: Int): Long {
        val unitFils = product.effectiveAmountMinor()
        if (unitFils <= 0L || requestedQuantity <= 0) return 0L

        val buy = product.buyUnits
        val free = product.freeUnits
        if (buy != null && free != null && buy >= 1 && free >= 1) {
            val bundleSize = buy + free
            val fullBundles = requestedQuantity / bundleSize
            val remainder = requestedQuantity % bundleSize
            val payableUnits = (fullBundles * buy) + remainder
            return unitFils * payableUnits
        }

        return unitFils * requestedQuantity
    }

    fun computeUnitPriceText(product: ProductEntity): String? {
        val unit = ProductIdentityMatcher.extractCanonicalUnit(product) ?: return null
        val fils = product.effectiveAmountMinor()
        if (fils <= 0L) return null
        val totalBaseUnits = unit.baseQuantity * unit.packCount.coerceAtLeast(1)
        if (totalBaseUnits <= 0.0) return null

        return when (unit.baseUnit) {
            "g" -> {
                val perKgFils = ((fils.toDouble() / totalBaseUnits) * 1000.0).toLong()
                "${Money.formatAed(perKgFils)} / kg"
            }
            "ml" -> {
                val perLitreFils = ((fils.toDouble() / totalBaseUnits) * 1000.0).toLong()
                "${Money.formatAed(perLitreFils)} / L"
            }
            "each" -> {
                val perEachFils = (fils.toDouble() / totalBaseUnits).toLong()
                "${Money.formatAed(perEachFils)} / unit"
            }
            else -> null
        }
    }

    /**
     * Selects deterministically among conflicting eligible offers in the same store for one requested line.
     */
    fun pickBestOfferForStore(
        requested: RequestedItem,
        storeProducts: List<ProductEntity>,
        request: BasketRequest,
        referenceProduct: ProductEntity? = null
    ): PricedOfferEvaluation? {
        val candidates = storeProducts.mapNotNull { prod ->
            if (!isProductEligibleForRequest(prod, requested.requestedQuantity, request)) return@mapNotNull null
            val reason = ProductIdentityMatcher.matchReason(requested, prod, referenceProduct) ?: return@mapNotNull null
            val subtotal = computeLineSubtotalFils(prod, requested.requestedQuantity)
            if (subtotal <= 0L) return@mapNotNull null
            PricedOfferEvaluation(
                product = prod,
                lineSubtotalFils = subtotal,
                unitPriceText = computeUnitPriceText(prod),
                matchReason = reason
            )
        }

        return candidates.minWithOrNull(
            compareBy<PricedOfferEvaluation> { it.lineSubtotalFils }
                .thenByDescending { it.product.lastVerifiedAt ?: it.product.updatedAt }
                .thenBy { it.product.id }
        )
    }
}
