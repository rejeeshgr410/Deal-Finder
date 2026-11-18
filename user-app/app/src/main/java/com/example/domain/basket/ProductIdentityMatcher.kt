package com.example.domain.basket

import com.example.data.OfferContractV2
import com.example.data.ProductEntity
import kotlin.math.abs

/**
 * Deterministic SKU identity and canonical unit/pack matcher.
 * Never auto-merges fuzzy text candidates or mismatched sizes/packs/variants.
 */
object ProductIdentityMatcher {

    private val VARIANT_MARKERS = listOf(
        "full cream", "low fat", "skimmed", "skim", "0% fat", "zero fat", "fat free",
        "long life", "uht", "fresh", "organic",
        "sugar free", "zero sugar", "no sugar", "diet", "light",
        "unsalted", "salted", "spicy", "original", "classic", "decaf",
        "large", "medium", "small", "xl"
    )

    private val CURATED_ALIASES: Map<String, String> = mapOf(
        "حليب المراعي طازج" to "almarai fresh milk",
        "حليب المراعي" to "almarai milk",
        "كوكاكولا" to "coca cola",
        "كوكا كولا" to "coca cola",
        "coca-cola" to "coca cola",
        "coke" to "coca cola",
        "ارز صنوايت" to "sunwhite calrose rice",
        "أرز صنوايت" to "sunwhite calrose rice",
        "بيض طازج" to "fresh eggs",
        "دجاج طازج" to "fresh chicken",
        "زيت نور" to "noor sunflower oil",
        "مسحوق تايد" to "tide washing powder",
        "نوتيلا" to "nutella hazelnut spread"
    )

    private val SIZE_IN_NAME_REGEX = Regex("""(\d+)\s*[xX×]\s*(\d+(?:\.\d+)?)\s*(ml|l|ltr|liter|litre|g|gm|kg|pcs|pc|pack|packs|can|cans|s)\b""", RegexOption.IGNORE_CASE)
    private val SINGLE_SIZE_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(ml|l|ltr|liter|litre|g|gm|kg|pcs|pc|pack|packs|can|cans)\b""", RegexOption.IGNORE_CASE)
    private val EGG_COUNT_REGEX = Regex("""\b(\d+)\s*(?:pcs|pc|pack|s|eggs)?\b""", RegexOption.IGNORE_CASE)

    fun extractCanonicalUnit(product: ProductEntity): CanonicalUnit? {
        // 1. Prefer explicit V2 canonical fields if present
        if (product.baseQuantity != null && product.baseQuantity > 0.0 && !product.baseUnit.isNullOrBlank()) {
            return CanonicalUnit(
                baseQuantity = product.baseQuantity,
                baseUnit = product.baseUnit.lowercase(),
                packCount = product.packCount.coerceAtLeast(1)
            )
        }

        // 2. Check pack notation inside product name (e.g., "330ml (Pack of 6)" or "6x330ml")
        val nameLower = product.name.lowercase()
        val packOfMatch = Regex("""pack\s+of\s+(\d+)""", RegexOption.IGNORE_CASE).find(nameLower)
        val explicitPack = product.packCount.coerceAtLeast(packOfMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1)

        val multiMatch = SIZE_IN_NAME_REGEX.find(nameLower)
        if (multiMatch != null) {
            val pack = multiMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            val rawVal = multiMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val rawUnit = multiMatch.groupValues[3]
            return toCanonical(rawVal, rawUnit, pack)
        }

        // 3. Explicit unitSize/unitType on V1 product (ignore default dummy 1.0 + "unit" if name specifies real size)
        val isDefaultV1Dummy = product.unitSize == 1.0 && product.unitType.equals("unit", ignoreCase = true)
        if (!isDefaultV1Dummy && product.unitSize > 0.0 && product.unitType.isNotBlank()) {
            val fromFields = toCanonical(product.unitSize, product.unitType, explicitPack)
            if (fromFields != null) return fromFields
        }

        // 4. Parse from product name as last deterministic extraction
        val singleMatch = SINGLE_SIZE_REGEX.find(nameLower)
        if (singleMatch != null) {
            val rawVal = singleMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val rawUnit = singleMatch.groupValues[2]
            return toCanonical(rawVal, rawUnit, explicitPack)
        }

        if (nameLower.contains("egg")) {
            val countMatch = Regex("""\b(\d+)\s*(?:pcs|pack|s)?\b""").find(nameLower)
            val cnt = countMatch?.groupValues?.get(1)?.toDoubleOrNull()
            if (cnt != null && cnt > 0) return CanonicalUnit(cnt, "each", explicitPack)
        }

        return null
    }

    fun extractCanonicalUnitFromIdentity(identity: ProductIdentity): CanonicalUnit? {
        if (identity.baseQuantity != null && identity.baseQuantity > 0.0 && !identity.baseUnit.isNullOrBlank()) {
            return CanonicalUnit(identity.baseQuantity, identity.baseUnit.lowercase(), identity.packCount.coerceAtLeast(1))
        }
        if (identity.name.isBlank()) return null
        return extractCanonicalUnit(
            ProductEntity(
                name = identity.name,
                brand = identity.brand,
                packCount = identity.packCount.coerceAtLeast(1)
            )
        )
    }

    fun toCanonical(rawSize: Double, rawUnit: String, packCount: Int = 1): CanonicalUnit? {
        if (!rawSize.isFinite() || rawSize <= 0.0) return null
        val pack = packCount.coerceAtLeast(1)
        return when (rawUnit.trim().lowercase()) {
            "g", "gm", "gram", "grams" -> CanonicalUnit(rawSize, "g", pack)
            "kg", "kgs", "kilogram", "kilograms" -> CanonicalUnit(rawSize * 1000.0, "g", pack)
            "ml" -> CanonicalUnit(rawSize, "ml", pack)
            "l", "ltr", "liter", "liters", "litre", "litres" -> CanonicalUnit(rawSize * 1000.0, "ml", pack)
            "each", "ea", "pc", "pcs", "piece", "pieces", "unit", "can", "cans", "bottle", "bottles", "pack", "packs", "s" ->
                CanonicalUnit(rawSize, "each", pack)
            else -> null
        }
    }

    fun areUnitsCompatible(a: CanonicalUnit?, b: CanonicalUnit?): Boolean {
        if (a == null || b == null) return a == null && b == null
        if (a.baseUnit != b.baseUnit) return false
        if (a.packCount != b.packCount) return false
        return abs(a.baseQuantity - b.baseQuantity) <= 0.01
    }

    fun applyAliases(raw: String): String {
        var s = raw.trim().lowercase()
        for ((alias, canonical) in CURATED_ALIASES) {
            if (s.contains(alias)) {
                s = s.replace(alias, canonical)
            }
        }
        return s
    }

    fun extractVariants(text: String): Set<String> {
        val lower = applyAliases(text)
        return VARIANT_MARKERS.filter { lower.contains(it) }.toSet()
    }

    fun normalizeCoreName(raw: String): String {
        var s = applyAliases(raw)
        // Strip unit expressions and punctuation so core identity can be compared cleanly
        s = SIZE_IN_NAME_REGEX.replace(s, " ")
        s = SINGLE_SIZE_REGEX.replace(s, " ")
        s = s.replace(Regex("""pack\s+of\s+\d+""", RegexOption.IGNORE_CASE), " ")
        s = s.replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
        return s.replace(Regex("""\s+"""), " ").trim()
    }

    fun areVariantsCompatible(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return true
        val va = extractVariants(a)
        val vb = extractVariants(b)
        return va == vb
    }

    fun areExactSameSku(a: ProductEntity, b: ProductEntity): Boolean {
        if (a.id.isNotBlank() && a.id == b.id) return true

        val unitA = extractCanonicalUnit(a)
        val unitB = extractCanonicalUnit(b)
        val unitsCompatible = if (unitA != null || unitB != null) areUnitsCompatible(unitA, unitB) else true
        if (!unitsCompatible) return false
        if (!areVariantsCompatible(a.name, b.name)) return false

        // 1. Exact verified GTIN/barcode + compatible unit/pack
        if (OfferContractV2.isValidGtin(a.barcode) && OfferContractV2.isValidGtin(b.barcode)) {
            if (a.barcode.trim() == b.barcode.trim()) return true
        }

        // 2. Verified masterProductId + compatible unit/pack
        if (a.masterProductId.isNotBlank() && b.masterProductId.isNotBlank()) {
            if (a.masterProductId.trim() == b.masterProductId.trim()) return true
        }

        // 3. Exact curated alias / normalized brand + core name + exact unit/pack
        val normA = normalizeCoreName(a.name)
        val normB = normalizeCoreName(b.name)
        if (normA.isNotBlank() && normA == normB) {
            val brandA = a.brand.trim().lowercase()
            val brandB = b.brand.trim().lowercase()
            if (brandA.isBlank() || brandB.isBlank() || brandA == brandB) {
                return true
            }
        }

        return false
    }

    /**
     * Matches a requested shopping-list item against a candidate product offer.
     * Returns match reason ("GTIN", "MASTER_ID", "EXACT_ALIAS_AND_PACK") or null if not an exact match.
     */
    fun matchReason(requested: RequestedItem, candidate: ProductEntity, referenceProduct: ProductEntity? = null): String? {
        if (referenceProduct != null) {
            return if (areExactSameSku(referenceProduct, candidate)) {
                when {
                    OfferContractV2.isValidGtin(referenceProduct.barcode) &&
                        referenceProduct.barcode.trim() == candidate.barcode.trim() -> "GTIN"
                    referenceProduct.masterProductId.isNotBlank() &&
                        referenceProduct.masterProductId == candidate.masterProductId -> "MASTER_ID"
                    else -> "EXACT_SKU_AND_PACK"
                }
            } else {
                null
            }
        }

        val reqUnit = extractCanonicalUnitFromIdentity(requested.identity)
        val candUnit = extractCanonicalUnit(candidate)
        if (reqUnit != null && candUnit != null && !areUnitsCompatible(reqUnit, candUnit)) {
            return null
        }
        if (reqUnit != null && candUnit == null) {
            return null
        }

        val reqFullText = "${requested.identity.name.ifBlank { requested.name }} ${requested.identity.variant}".trim()
        if (!areVariantsCompatible(reqFullText, candidate.name)) {
            return null
        }

        if (OfferContractV2.isValidGtin(requested.identity.barcodeGtin) &&
            OfferContractV2.isValidGtin(candidate.barcode) &&
            requested.identity.barcodeGtin.trim() == candidate.barcode.trim()
        ) {
            return "GTIN"
        }

        if (requested.identity.masterProductId.isNotBlank() &&
            candidate.masterProductId == requested.identity.masterProductId
        ) {
            return "MASTER_ID"
        }

        val reqNorm = normalizeCoreName(requested.identity.name.ifBlank { requested.name })
        val candNorm = normalizeCoreName(candidate.name)
        if (reqNorm.isNotBlank() && reqNorm == candNorm) {
            val reqBrand = requested.identity.brand.trim().lowercase()
            val candBrand = candidate.brand.trim().lowercase()
            if (reqBrand.isBlank() || candBrand.isBlank() || reqBrand == candBrand) {
                return "EXACT_ALIAS_AND_PACK"
            }
        }

        return null
    }

    /**
     * Suggestions only for manual user selection (never auto-merged into basket totals).
     */
    fun suggestCandidates(requested: RequestedItem, candidates: List<ProductEntity>, limit: Int = 5): List<ProductEntity> {
        val reqTokens = normalizeCoreName(requested.name).split(" ").filter { it.length > 2 }.toSet()
        if (reqTokens.isEmpty()) return emptyList()
        return candidates
            .mapNotNull { cand ->
                val candTokens = normalizeCoreName(cand.name).split(" ").filter { it.length > 2 }.toSet()
                val overlap = reqTokens.intersect(candTokens).size
                if (overlap > 0) cand to overlap else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
