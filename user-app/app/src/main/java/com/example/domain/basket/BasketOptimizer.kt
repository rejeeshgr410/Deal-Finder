package com.example.domain.basket

import com.example.data.ProductEntity
import com.example.data.RetailerEntity

/**
 * Pure deterministic 1-store vs 2-store grocery basket optimizer.
 * Keyed strictly by RequestedItem.lineId; never substitutes targetPrice for unpriced items.
 */
object BasketOptimizer {

    const val MAX_CANDIDATE_STORES = 40

    fun optimize(
        request: BasketRequest,
        products: List<ProductEntity>,
        stores: List<RetailerEntity>
    ): BasketComparisonResult {
        if (request.items.isEmpty()) {
            return BasketComparisonResult(
                request = request,
                bestOneStore = null,
                bestTwoStore = null,
                preferredStoreOption = null,
                candidateStoreCount = 0,
                storesTruncated = false,
                recommendationSummary = "Add items to your basket to compare participating UAE retailers.",
                coverageDisclosure = "0 items requested."
            )
        }

        val eligibleStoresAll = stores
            .filter { s ->
                (s.status.equals("published", ignoreCase = true) || s.status.equals("Active", ignoreCase = true)) &&
                    (request.allowedStoreIds == null || s.id in request.allowedStoreIds)
            }
            .sortedBy { it.id }

        val storesTruncated = eligibleStoresAll.size > MAX_CANDIDATE_STORES
        val candidateStores = eligibleStoresAll.take(MAX_CANDIDATE_STORES)
        val storeMap = candidateStores.associateBy { it.id }
        val productsByStore = products
            .filter { it.retailerId in storeMap.keys }
            .groupBy { it.retailerId }

        val productById = products.associateBy { it.id }

        // Evaluate all single-store plans
        val singleStoreOptions = candidateStores.mapNotNull { store ->
            evaluateStoreSet(
                storeIds = listOf(store.id),
                storeMap = storeMap,
                productsByStore = productsByStore,
                productById = productById,
                request = request
            )
        }

        val bestOneStore = rankOptions(singleStoreOptions, request.objective).firstOrNull()

        // Evaluate all 2-store pairs only when maxStores >= 2 and at least 2 candidate stores exist
        val twoStoreOptions = mutableListOf<BasketOption>()
        if (request.maxStores >= 2 && candidateStores.size >= 2) {
            for (i in 0 until candidateStores.size) {
                for (j in (i + 1) until candidateStores.size) {
                    val s1 = candidateStores[i].id
                    val s2 = candidateStores[j].id
                    val option = evaluateStoreSet(
                        storeIds = listOf(s1, s2),
                        storeMap = storeMap,
                        productsByStore = productsByStore,
                        productById = productById,
                        request = request
                    )
                    if (option != null && option.storeIds.distinct().size == 2) {
                        twoStoreOptions.add(option)
                    }
                }
            }
        }

        val rawBestTwoStore = rankOptions(twoStoreOptions, request.objective).firstOrNull()
        val bestTwoStore = rawBestTwoStore?.let { twoOpt ->
            attachBaselineComparison(twoOpt, bestOneStore)
        }

        val preferredStoreOption = request.preferredRetailerId?.let { prefId ->
            singleStoreOptions.find { it.storeIds == listOf(prefId) }
        }

        val coverageDisclosure = BasketExplanations.buildCoverageDisclosure(
            candidateStoreCount = candidateStores.size,
            selectedEmirate = request.selectedEmirate,
            storesTruncated = storesTruncated
        )
        val recommendationSummary = BasketExplanations.buildRecommendationSummary(
            objective = request.objective,
            bestOneStore = bestOneStore,
            bestTwoStore = bestTwoStore,
            preferredOption = preferredStoreOption
        )

        return BasketComparisonResult(
            request = request,
            bestOneStore = bestOneStore,
            bestTwoStore = bestTwoStore,
            preferredStoreOption = preferredStoreOption,
            candidateStoreCount = candidateStores.size,
            storesTruncated = storesTruncated,
            recommendationSummary = recommendationSummary,
            coverageDisclosure = coverageDisclosure
        )
    }

    private fun evaluateStoreSet(
        storeIds: List<String>,
        storeMap: Map<String, RetailerEntity>,
        productsByStore: Map<String, List<ProductEntity>>,
        productById: Map<String, ProductEntity>,
        request: BasketRequest
    ): BasketOption? {
        val pricedLines = mutableListOf<MatchedLine>()
        val unpricedLines = mutableListOf<UnpricedLine>()
        val usedStores = linkedSetOf<String>()
        var subtotalFils = 0L

        for (item in request.items) {
            val refProduct = item.referenceProductId.takeIf { it.isNotBlank() }?.let { productById[it] }
            val storeChoices = storeIds.mapNotNull { sid ->
                val eval = OfferEligibility.pickBestOfferForStore(
                    requested = item,
                    storeProducts = productsByStore[sid].orEmpty(),
                    request = request,
                    referenceProduct = refProduct
                )
                if (eval != null) sid to eval else null
            }

            val bestChoice = storeChoices.minWithOrNull(
                compareBy<Pair<String, PricedOfferEvaluation>> { it.second.lineSubtotalFils }
                    .thenBy { it.first }
            )

            if (bestChoice == null) {
                unpricedLines.add(
                    UnpricedLine(
                        lineId = item.lineId,
                        itemName = item.name,
                        reason = "No eligible verified offer in selected store(s)."
                    )
                )
            } else {
                val (chosenStoreId, eval) = bestChoice
                usedStores.add(chosenStoreId)
                subtotalFils += eval.lineSubtotalFils
                val storeName = storeMap[chosenStoreId]?.name ?: chosenStoreId
                val availabilityText = when (eval.product.availability) {
                    "retailer_reported" -> "Availability reported by retailer"
                    else -> "Offer price shown; stock not confirmed"
                }
                pricedLines.add(
                    MatchedLine(
                        lineId = item.lineId,
                        itemName = item.name,
                        storeId = chosenStoreId,
                        storeName = storeName,
                        product = eval.product,
                        requestedQuantity = item.requestedQuantity,
                        lineSubtotalFils = eval.lineSubtotalFils,
                        matchReason = eval.matchReason,
                        unitPriceText = eval.unitPriceText,
                        verificationLabel = eval.product.verificationLabel.ifBlank { "unverified" },
                        availabilityDisclosure = availabilityText
                    )
                )
            }
        }

        if (pricedLines.isEmpty()) return null

        val activeStoreIds = if (usedStores.isNotEmpty()) usedStores.toList() else storeIds
        val activeStoreNames = activeStoreIds.map { storeMap[it]?.name ?: it }
        val complete = unpricedLines.isEmpty()

        val allFeesKnown = activeStoreIds.all { request.knownFeesByStoreFils.containsKey(it) }
        val knownFeesFils: Long? = if (request.knownFeesByStoreFils.isEmpty()) {
            null
        } else if (allFeesKnown) {
            activeStoreIds.sumOf { request.knownFeesByStoreFils[it] ?: 0L }
        } else {
            null
        }

        val unknownFeeDisclosure = if (knownFeesFils == null) {
            "Listed product prices only; travel/delivery fees not included."
        } else {
            null
        }

        // totalFils MUST be null when basket coverage is incomplete
        val totalFils: Long? = if (!complete) {
            null
        } else {
            subtotalFils + (knownFeesFils ?: 0L)
        }

        val warnings = mutableListOf<String>()
        if (!complete) {
            warnings.add("${pricedLines.size} of ${request.items.size} items priced. Full basket total unavailable.")
        }
        if (pricedLines.any { it.product.availability != "retailer_reported" }) {
            warnings.add("Offer price shown; stock at branch not confirmed.")
        }
        if (pricedLines.any { it.product.packCount > 1 }) {
            warnings.add("Multipack pricing included; check pack counts per line.")
        }

        return BasketOption(
            storeIds = activeStoreIds,
            storeNames = activeStoreNames,
            pricedLines = pricedLines,
            unpricedLines = unpricedLines,
            complete = complete,
            itemCoverage = pricedLines.size,
            itemCount = request.items.size,
            verifiedItemsSubtotalFils = subtotalFils,
            knownFeesFils = knownFeesFils,
            totalFils = totalFils,
            unknownFeeDisclosure = unknownFeeDisclosure,
            comparisonDeltaVsBestSingleStoreFils = null,
            savingsBaselineDisclosure = "Baseline comparison not applicable.",
            sourceSnapshotAtMs = request.nowMs,
            explanatoryWarnings = warnings
        )
    }

    private fun attachBaselineComparison(twoOpt: BasketOption, bestOneStore: BasketOption?): BasketOption {
        if (bestOneStore == null || !twoOpt.complete || !bestOneStore.complete) {
            return twoOpt.copy(
                comparisonDeltaVsBestSingleStoreFils = null,
                savingsBaselineDisclosure = "Savings not measurable yet (incomplete basket coverage)."
            )
        }

        val oneTotal = bestOneStore.totalFils ?: return twoOpt
        val twoTotal = twoOpt.totalFils ?: return twoOpt
        val delta = oneTotal - twoTotal // positive means 2-store is cheaper

        if (delta <= 0L) {
            return twoOpt.copy(
                comparisonDeltaVsBestSingleStoreFils = 0L,
                savingsBaselineDisclosure = "No savings vs lowest complete 1-store basket (${bestOneStore.storeNames.firstOrNull() ?: "single store"})."
            )
        }

        val feeSuffix = if (twoOpt.knownFeesFils == null) {
            "travel cost not included."
        } else {
            "including known fees."
        }

        return twoOpt.copy(
            comparisonDeltaVsBestSingleStoreFils = delta,
            savingsBaselineDisclosure = "${Money.formatAed(delta)} lower in listed product prices vs ${bestOneStore.storeNames.firstOrNull() ?: "best 1-store"}; $feeSuffix"
        )
    }

    private fun rankOptions(options: List<BasketOption>, objective: Objective): List<BasketOption> {
        return options.sortedWith(
            compareByDescending<BasketOption> { it.complete }
                .thenByDescending { it.itemCoverage }
                .thenBy { option ->
                    when (objective) {
                        Objective.FEWEST_STORES -> option.storeIds.size.toLong()
                        Objective.PREFERRED_RETAILER -> 0L
                        Objective.LOWEST_PRICE -> 0L
                    }
                }
                .thenBy { it.totalFils ?: it.verifiedItemsSubtotalFils }
                .thenBy { it.storeIds.joinToString(",") }
        )
    }
}
