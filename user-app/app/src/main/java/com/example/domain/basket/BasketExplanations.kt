package com.example.domain.basket

object BasketExplanations {

    fun buildCoverageDisclosure(
        candidateStoreCount: Int,
        selectedEmirate: String,
        storesTruncated: Boolean
    ): String {
        val base = "Comparing $candidateStoreCount participating store(s) in $selectedEmirate."
        return if (storesTruncated) {
            "$base Search capped at ${BasketOptimizer.MAX_CANDIDATE_STORES} stores."
        } else {
            base
        }
    }

    fun buildRecommendationSummary(
        objective: Objective,
        bestOneStore: BasketOption?,
        bestTwoStore: BasketOption?,
        preferredOption: BasketOption?
    ): String {
        if (bestOneStore == null && bestTwoStore == null) {
            return "No eligible published offers matched your basket items."
        }

        if (objective == Objective.PREFERRED_RETAILER && preferredOption != null) {
            val totalText = preferredOption.totalFils?.let { Money.formatAed(it) }
                ?: "Subtotal ${Money.formatAed(preferredOption.verifiedItemsSubtotalFils)} (${preferredOption.itemCoverage}/${preferredOption.itemCount} priced)"
            return "Preferred retailer ${preferredOption.storeNames.firstOrNull()}: $totalText."
        }

        if (objective == Objective.FEWEST_STORES && bestOneStore != null && bestOneStore.complete) {
            return "Best 1-store convenience at ${bestOneStore.storeNames.first()}: ${Money.formatAed(bestOneStore.totalFils ?: bestOneStore.verifiedItemsSubtotalFils)}."
        }

        if (bestTwoStore != null && bestOneStore != null && bestTwoStore.complete && bestOneStore.complete) {
            val delta = bestTwoStore.comparisonDeltaVsBestSingleStoreFils ?: 0L
            if (delta > 0L) {
                return "2-store split (${bestTwoStore.storeNames.joinToString(" + ")}) is ${Money.formatAed(delta)} lower in listed product prices than 1-store (${bestOneStore.storeNames.first()}); travel cost not included."
            }
            return "1-store (${bestOneStore.storeNames.first()}) matches or beats splitting stores at ${Money.formatAed(bestOneStore.totalFils ?: bestOneStore.verifiedItemsSubtotalFils)}."
        }

        val fallback = bestOneStore ?: bestTwoStore!!
        return if (!fallback.complete) {
            "Partial coverage: ${fallback.itemCoverage} of ${fallback.itemCount} items priced (known subtotal ${Money.formatAed(fallback.verifiedItemsSubtotalFils)})."
        } else {
            "Best available option: ${fallback.storeNames.joinToString(" + ")} (${Money.formatAed(fallback.totalFils ?: fallback.verifiedItemsSubtotalFils)})."
        }
    }

    fun explainDrivers(bestOneStore: BasketOption?, bestTwoStore: BasketOption?): List<String> {
        if (bestOneStore == null || bestTwoStore == null) return emptyList()
        val oneByLine = bestOneStore.pricedLines.associateBy { it.lineId }
        val drivers = mutableListOf<String>()
        for (twoLine in bestTwoStore.pricedLines) {
            val oneLine = oneByLine[twoLine.lineId] ?: continue
            val diff = oneLine.lineSubtotalFils - twoLine.lineSubtotalFils
            if (diff > 0L) {
                drivers.add("${twoLine.itemName}: ${Money.formatAed(diff)} lower at ${twoLine.storeName} (${Money.formatAed(twoLine.lineSubtotalFils)} vs ${Money.formatAed(oneLine.lineSubtotalFils)})")
            }
        }
        return drivers
    }
}
