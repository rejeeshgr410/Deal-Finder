package com.example.demo

import android.content.Context
import com.example.R
import com.example.data.*
import kotlin.math.roundToLong

/**
 * Self-contained temporary UAE Demo Catalog & Asset Bundle.
 * Loaded dynamically by [DemoRegistry] via reflection so `src/demo/`
 * can be disabled at runtime or removed entirely when switching to live data only.
 */
class DemoDataBundle : DemoDataProvider {

    override fun buildSnapshot(context: Context): DemoCatalogSnapshot {
        val pkg = context.packageName
        fun resUri(resId: Int): String = "android.resource://$pkg/$resId"

        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val allEmirates = "Dubai, Abu Dhabi, Sharjah, Ajman, Ras Al Khaimah, Fujairah, Umm Al Quwain"

        val luluLogo = resUri(R.drawable.ic_fig_logo_lulu)
        val carrefourLogo = resUri(R.drawable.ic_fig_logo_carrefour)
        val nestoLogo = resUri(R.drawable.ic_fig_logo_nesto)
        val vivaLogo = resUri(R.drawable.ic_fig_logo_viva)

        val bananaImg = resUri(R.drawable.fig_prod_banana)
        val noorOilImg = resUri(R.drawable.fig_prod_noor_oil)
        val basmatiImg = resUri(R.drawable.fig_prod_india_gate)
        val milkImg = resUri(R.drawable.fig_prod_al_ain_milk)

        val nescafeClassicImg = resUri(R.drawable.demo_prod_nescafe_classic)
        val nescafeGoldImg = resUri(R.drawable.demo_prod_nescafe_gold)
        val nescafe3in1Img = resUri(R.drawable.demo_prod_nescafe_3in1)
        val nescafeIcedLatteImg = resUri(R.drawable.demo_prod_nescafe_iced_latte)
        val kitkatImg = resUri(R.drawable.demo_prod_nestle_kitkat)
        val maggiImg = resUri(R.drawable.demo_prod_maggi_noodles)
        val pureLifeImg = resUri(R.drawable.demo_prod_nestle_pure_life)
        val smartiesImg = resUri(R.drawable.demo_prod_nestle_smarties)
        val spellegrinoImg = resUri(R.drawable.demo_prod_spellegrino)
        val friskiesImg = resUri(R.drawable.demo_prod_purina_friskies)

        val retailers = listOf(
            RetailerEntity(
                id = "demo_ret_lulu",
                name = "LuLu Hypermarket",
                logoUrl = luluLogo,
                status = "published",
                emirates = allEmirates,
                branchesCount = 14,
                searchKeywords = listOf("lulu", "hypermarket", "groceries", "fresh"),
                createdAt = now - 30 * dayMs,
                updatedAt = now
            ),
            RetailerEntity(
                id = "demo_ret_carrefour",
                name = "Carrefour",
                logoUrl = carrefourLogo,
                status = "published",
                emirates = allEmirates,
                branchesCount = 18,
                searchKeywords = listOf("carrefour", "maf", "hypermarket", "share"),
                createdAt = now - 30 * dayMs,
                updatedAt = now
            ),
            RetailerEntity(
                id = "demo_ret_nesto",
                name = "Nesto Hypermarket",
                logoUrl = nestoLogo,
                status = "published",
                emirates = allEmirates,
                branchesCount = 12,
                searchKeywords = listOf("nesto", "inaam", "hypermarket", "value"),
                createdAt = now - 30 * dayMs,
                updatedAt = now
            ),
            RetailerEntity(
                id = "demo_ret_viva",
                name = "Viva Supermarket",
                logoUrl = vivaLogo,
                status = "published",
                emirates = allEmirates,
                branchesCount = 10,
                searchKeywords = listOf("viva", "supermarket", "discount", "fresh"),
                createdAt = now - 30 * dayMs,
                updatedAt = now
            )
        )

        val branches = listOf(
            BranchEntity(
                id = "demo_br_lulu_barsha",
                retailerId = "demo_ret_lulu",
                name = "LuLu Hypermarket - Al Barsha",
                emirate = "Dubai",
                address = "Al Barsha 1, Near Mall of the Emirates, Dubai",
                latitude = 25.1145,
                longitude = 55.1972,
                phone = "+971 4 341 8888",
                openingHours = "08:00 AM - 12:00 AM",
                coordsVerified = true,
                coordSource = "demo_seed"
            ),
            BranchEntity(
                id = "demo_br_lulu_ad",
                retailerId = "demo_ret_lulu",
                name = "LuLu Hypermarket - Al Wahda Mall",
                emirate = "Abu Dhabi",
                address = "Al Wahda Mall, Hazza Bin Zayed St, Abu Dhabi",
                latitude = 24.4709,
                longitude = 54.3728,
                phone = "+971 2 443 7500",
                openingHours = "08:00 AM - 12:00 AM",
                coordsVerified = true,
                coordSource = "demo_seed"
            ),
            BranchEntity(
                id = "demo_br_crf_moe",
                retailerId = "demo_ret_carrefour",
                name = "Carrefour - Mall of the Emirates",
                emirate = "Dubai",
                address = "Sheikh Zayed Road, Al Barsha, Dubai",
                latitude = 25.1181,
                longitude = 55.2006,
                phone = "800 73232",
                openingHours = "09:00 AM - 12:00 AM",
                coordsVerified = true,
                coordSource = "demo_seed"
            ),
            BranchEntity(
                id = "demo_br_crf_shj",
                retailerId = "demo_ret_carrefour",
                name = "Carrefour - City Centre Sharjah",
                emirate = "Sharjah",
                address = "Al Wahda St, Industrial Area, Sharjah",
                latitude = 25.3254,
                longitude = 55.3932,
                phone = "800 73232",
                openingHours = "09:00 AM - 12:00 AM",
                coordsVerified = true,
                coordSource = "demo_seed"
            ),
            BranchEntity(
                id = "demo_br_nesto_deira",
                retailerId = "demo_ret_nesto",
                name = "Nesto Hypermarket - Al Rigga",
                emirate = "Dubai",
                address = "Al Rigga Road, Deira, Dubai",
                latitude = 25.2632,
                longitude = 55.3228,
                phone = "+971 4 221 9000",
                openingHours = "08:00 AM - 12:00 AM",
                coordsVerified = true,
                coordSource = "demo_seed"
            ),
            BranchEntity(
                id = "demo_br_viva_jvc",
                retailerId = "demo_ret_viva",
                name = "Viva Supermarket - JVC",
                emirate = "Dubai",
                address = "District 13, Jumeirah Village Circle, Dubai",
                latitude = 25.0598,
                longitude = 55.2094,
                phone = "+971 4 580 1200",
                openingHours = "07:00 AM - 11:30 PM",
                coordsVerified = true,
                coordSource = "demo_seed"
            )
        )

        val flyers = listOf(
            FlyerEntity(
                id = "demo_flyer_lulu",
                retailerId = "demo_ret_lulu",
                title = "LuLu Super Saver Fresh Deals",
                imageUrl = luluLogo,
                pdfUrl = "",
                pages = 16,
                status = "published",
                searchKeywords = listOf("lulu", "banana", "rice", "milk", "oil", "fresh"),
                validFrom = now - 2 * dayMs,
                validTo = now + 2 * dayMs + 3_600_000L,
                views = 2840,
                clicks = 610,
                createdAt = now - 2 * dayMs,
                updatedAt = now
            ),
            FlyerEntity(
                id = "demo_flyer_carrefour",
                retailerId = "demo_ret_carrefour",
                title = "Carrefour Mid-Week Big Savings",
                imageUrl = carrefourLogo,
                pdfUrl = "",
                pages = 20,
                status = "published",
                searchKeywords = listOf("carrefour", "noor", "oil", "grocery", "electronics"),
                validFrom = now - dayMs,
                validTo = now + 4 * dayMs + 3_600_000L,
                views = 3420,
                clicks = 890,
                createdAt = now - dayMs,
                updatedAt = now
            ),
            FlyerEntity(
                id = "demo_flyer_nesto",
                retailerId = "demo_ret_nesto",
                title = "Nesto Hypermarket Flash Offers",
                imageUrl = nestoLogo,
                pdfUrl = "",
                pages = 12,
                status = "published",
                searchKeywords = listOf("nesto", "basmati", "india gate", "pantry"),
                validFrom = now - 2 * dayMs,
                validTo = now + 1 * dayMs + 7_200_000L,
                views = 1950,
                clicks = 430,
                createdAt = now - 2 * dayMs,
                updatedAt = now
            ),
            FlyerEntity(
                id = "demo_flyer_viva",
                retailerId = "demo_ret_viva",
                title = "Viva Everyday Low Price Specials",
                imageUrl = vivaLogo,
                pdfUrl = "",
                pages = 10,
                status = "published",
                searchKeywords = listOf("viva", "al ain", "milk", "dairy", "fresh"),
                validFrom = now - dayMs,
                validTo = now + 3 * dayMs + 3_600_000L,
                views = 1520,
                clicks = 310,
                createdAt = now - dayMs,
                updatedAt = now
            )
        )

        fun demoProd(
            id: String,
            masterId: String,
            retailerId: String,
            flyerId: String,
            name: String,
            normName: String,
            brand: String,
            category: String,
            barcode: String,
            imageUrl: String,
            price: Double,
            oldPrice: Double?,
            unitSize: Double,
            unitType: String,
            baseQty: Double,
            baseUnit: String,
            packCount: Int = 1,
            extraKeywords: List<String> = emptyList(),
            loyaltyRequired: Boolean = false
        ) = ProductEntity(
            id = id,
            masterProductId = masterId,
            retailerId = retailerId,
            flyerId = flyerId,
            name = name,
            normalizedName = normName,
            searchKeywords = (listOf(name.lowercase(), normName, brand.lowercase(), category.lowercase()) + extraKeywords).distinct(),
            brand = brand,
            category = category,
            barcode = barcode,
            imageUrl = imageUrl,
            currentPrice = price,
            oldPrice = oldPrice,
            unitSize = unitSize,
            unitType = unitType,
            validFrom = now - 2 * dayMs,
            validTo = now + 5 * dayMs,
            status = "published",
            createdAt = now - 2 * dayMs,
            updatedAt = now,
            schemaVersion = 2,
            amountMinor = (price * 100.0).roundToLong(),
            comparisonPriceMinor = oldPrice?.let { (it * 100.0).roundToLong() },
            baseQuantity = baseQty,
            baseUnit = baseUnit,
            packCount = packCount,
            minUnits = 1,
            loyaltyRequired = loyaltyRequired,
            availability = "in_stock",
            sourceType = "demo",
            sourceLabel = "UAE Demo Catalog",
            verificationLabel = "verified",
            lastVerifiedAt = now,
            isDemo = true
        )

        val nestleKeywords = listOf("nestle", "nestlé", "trending")

        fun nestleMultiOfferProducts(
            slug: String,
            masterId: String,
            name: String,
            normName: String,
            brand: String,
            category: String,
            barcode: String,
            imageUrl: String,
            unitSize: Double,
            unitType: String,
            baseQty: Double,
            baseUnit: String,
            packCount: Int = 1,
            extraKeywords: List<String> = emptyList(),
            offers: List<Triple<String, Double, Double>>
        ): List<ProductEntity> {
            val flyerByRetailer = mapOf(
                "demo_ret_lulu" to "demo_flyer_lulu",
                "demo_ret_carrefour" to "demo_flyer_carrefour",
                "demo_ret_nesto" to "demo_flyer_nesto",
                "demo_ret_viva" to "demo_flyer_viva"
            )
            return offers.map { (retailerId, price, oldPrice) ->
                val shortRet = retailerId.removePrefix("demo_ret_")
                demoProd(
                    id = "demo_prod_${slug}_$shortRet",
                    masterId = masterId,
                    retailerId = retailerId,
                    flyerId = flyerByRetailer[retailerId] ?: "demo_flyer_lulu",
                    name = name,
                    normName = normName,
                    brand = brand,
                    category = category,
                    barcode = barcode,
                    imageUrl = imageUrl,
                    price = price,
                    oldPrice = oldPrice,
                    unitSize = unitSize,
                    unitType = unitType,
                    baseQty = baseQty,
                    baseUnit = baseUnit,
                    packCount = packCount,
                    extraKeywords = nestleKeywords + extraKeywords
                )
            }
        }

        val nestleShowcaseProducts = listOf(
            // 1. Nescafé Gold Instant Coffee 200g
            nestleMultiOfferProducts(
                slug = "nescafe_gold",
                masterId = "master_nescafe_gold_200g",
                name = "Nescafé Gold Instant Coffee 200g",
                normName = "nescafe gold instant coffee 200g",
                brand = "Nescafé",
                category = "Groceries",
                barcode = "7613035012011",
                imageUrl = nescafeGoldImg,
                unitSize = 200.0,
                unitType = "g",
                baseQty = 200.0,
                baseUnit = "g",
                extraKeywords = listOf("nescafe", "nescafé", "coffee", "gold"),
                offers = listOf(
                    Triple("demo_ret_carrefour", 31.95, 42.50),
                    Triple("demo_ret_lulu", 33.50, 42.50),
                    Triple("demo_ret_nesto", 34.90, 42.50)
                )
            ),
            // 2. Nescafé Classic Instant Coffee 200g
            nestleMultiOfferProducts(
                slug = "nescafe_classic",
                masterId = "master_nescafe_classic_200g",
                name = "Nescafé Classic Instant Coffee 200g",
                normName = "nescafe classic instant coffee 200g",
                brand = "Nescafé",
                category = "Groceries",
                barcode = "7613035012028",
                imageUrl = nescafeClassicImg,
                unitSize = 200.0,
                unitType = "g",
                baseQty = 200.0,
                baseUnit = "g",
                extraKeywords = listOf("nescafe", "nescafé", "coffee", "classic"),
                offers = listOf(
                    Triple("demo_ret_nesto", 18.95, 26.50),
                    Triple("demo_ret_viva", 19.50, 25.00),
                    Triple("demo_ret_lulu", 19.90, 26.50),
                    Triple("demo_ret_carrefour", 21.50, 26.50)
                )
            ),
            // 3. Nescafé 3in1 Original Coffee Mix 24x20g
            nestleMultiOfferProducts(
                slug = "nescafe_3in1",
                masterId = "master_nescafe_3in1_24s",
                name = "Nescafé 3in1 Original 24x20g",
                normName = "nescafe 3in1 original 24x20g",
                brand = "Nescafé",
                category = "Groceries",
                barcode = "7613035012035",
                imageUrl = nescafe3in1Img,
                unitSize = 20.0,
                unitType = "g",
                baseQty = 480.0,
                baseUnit = "g",
                packCount = 24,
                extraKeywords = listOf("nescafe", "nescafé", "3in1", "coffee mix"),
                offers = listOf(
                    Triple("demo_ret_lulu", 15.90, 21.90),
                    Triple("demo_ret_nesto", 16.50, 21.90),
                    Triple("demo_ret_carrefour", 17.25, 21.90)
                )
            ),
            // 4. Nestlé KitKat 4-Finger Chocolate 12x41.5g
            nestleMultiOfferProducts(
                slug = "kitkat_12pk",
                masterId = "master_nestle_kitkat_12pk",
                name = "Nestlé KitKat 4-Finger 12x41.5g",
                normName = "nestle kitkat 4 finger 12x41.5g",
                brand = "Nestlé",
                category = "Groceries",
                barcode = "7613035012042",
                imageUrl = kitkatImg,
                unitSize = 41.5,
                unitType = "g",
                baseQty = 498.0,
                baseUnit = "g",
                packCount = 12,
                extraKeywords = listOf("kitkat", "chocolate", "snack"),
                offers = listOf(
                    Triple("demo_ret_carrefour", 22.95, 31.50),
                    Triple("demo_ret_viva", 23.50, 31.50),
                    Triple("demo_ret_nesto", 23.90, 31.50),
                    Triple("demo_ret_lulu", 24.90, 31.50)
                )
            ),
            // 5. Maggi 2-Minute Noodles Chicken 5x77g
            nestleMultiOfferProducts(
                slug = "maggi_noodles",
                masterId = "master_maggi_noodles_5pk",
                name = "Maggi 2-Minute Noodles 5x77g",
                normName = "maggi 2 minute noodles 5x77g",
                brand = "Nestlé Maggi",
                category = "Groceries",
                barcode = "7613035012059",
                imageUrl = maggiImg,
                unitSize = 77.0,
                unitType = "g",
                baseQty = 385.0,
                baseUnit = "g",
                packCount = 5,
                extraKeywords = listOf("maggi", "noodles", "instant noodles"),
                offers = listOf(
                    Triple("demo_ret_viva", 6.95, 9.50),
                    Triple("demo_ret_nesto", 7.25, 9.50),
                    Triple("demo_ret_lulu", 7.50, 9.50),
                    Triple("demo_ret_carrefour", 7.95, 9.50)
                )
            ),
            // 6. Nestlé Pure Life Water 1.5L (Pack of 6)
            nestleMultiOfferProducts(
                slug = "pure_life_water",
                masterId = "master_nestle_pure_life_6x1_5l",
                name = "Nestlé Pure Life Water 6x1.5L",
                normName = "nestle pure life water 6x1.5l",
                brand = "Nestlé",
                category = "Groceries",
                barcode = "7613035012066",
                imageUrl = pureLifeImg,
                unitSize = 1.5,
                unitType = "L",
                baseQty = 9000.0,
                baseUnit = "ml",
                packCount = 6,
                extraKeywords = listOf("pure life", "water", "drinking water"),
                offers = listOf(
                    Triple("demo_ret_viva", 6.50, 8.95),
                    Triple("demo_ret_nesto", 6.95, 8.95),
                    Triple("demo_ret_lulu", 7.25, 8.95),
                    Triple("demo_ret_carrefour", 7.50, 8.95)
                )
            ),
            // 7. Nescafé Iced Latte Can 240ml
            nestleMultiOfferProducts(
                slug = "nescafe_iced_latte",
                masterId = "master_nescafe_iced_latte_240ml",
                name = "Nescafé Iced Latte Can 240ml",
                normName = "nescafe iced latte can 240ml",
                brand = "Nescafé",
                category = "Groceries",
                barcode = "7613035012073",
                imageUrl = nescafeIcedLatteImg,
                unitSize = 240.0,
                unitType = "ml",
                baseQty = 240.0,
                baseUnit = "ml",
                extraKeywords = listOf("nescafe", "nescafé", "iced latte", "cold coffee"),
                offers = listOf(
                    Triple("demo_ret_carrefour", 5.50, 7.50),
                    Triple("demo_ret_nesto", 5.75, 7.50),
                    Triple("demo_ret_lulu", 5.95, 7.50)
                )
            ),
            // 8. Nestlé Smarties Sharing Pack 130g
            nestleMultiOfferProducts(
                slug = "smarties_130g",
                masterId = "master_nestle_smarties_130g",
                name = "Nestlé Smarties Sharing Box 130g",
                normName = "nestle smarties sharing box 130g",
                brand = "Nestlé",
                category = "Groceries",
                barcode = "7613035012080",
                imageUrl = smartiesImg,
                unitSize = 130.0,
                unitType = "g",
                baseQty = 130.0,
                baseUnit = "g",
                extraKeywords = listOf("smarties", "chocolate", "candy"),
                offers = listOf(
                    Triple("demo_ret_nesto", 8.95, 12.50),
                    Triple("demo_ret_lulu", 9.50, 12.50),
                    Triple("demo_ret_carrefour", 9.95, 12.50)
                )
            ),
            // 9. S.Pellegrino Sparkling Water 500ml
            nestleMultiOfferProducts(
                slug = "spellegrino_500ml",
                masterId = "master_spellegrino_500ml",
                name = "S.Pellegrino Sparkling Water 500ml",
                normName = "s pellegrino sparkling water 500ml",
                brand = "Nestlé Waters",
                category = "Groceries",
                barcode = "7613035012097",
                imageUrl = spellegrinoImg,
                unitSize = 500.0,
                unitType = "ml",
                baseQty = 500.0,
                baseUnit = "ml",
                extraKeywords = listOf("pellegrino", "sparkling water", "water"),
                offers = listOf(
                    Triple("demo_ret_carrefour", 4.50, 6.25),
                    Triple("demo_ret_lulu", 4.75, 6.25),
                    Triple("demo_ret_viva", 4.95, 6.25)
                )
            ),
            // 10. Purina Friskies Adult Cat Food 1.5kg
            nestleMultiOfferProducts(
                slug = "purina_friskies",
                masterId = "master_purina_friskies_1_5kg",
                name = "Purina Friskies Cat Food 1.5kg",
                normName = "purina friskies cat food 1.5kg",
                brand = "Nestlé Purina",
                category = "Groceries",
                barcode = "7613035012103",
                imageUrl = friskiesImg,
                unitSize = 1.5,
                unitType = "kg",
                baseQty = 1500.0,
                baseUnit = "g",
                extraKeywords = listOf("purina", "friskies", "cat food", "pet"),
                offers = listOf(
                    Triple("demo_ret_lulu", 19.90, 27.50),
                    Triple("demo_ret_nesto", 20.95, 27.50),
                    Triple("demo_ret_carrefour", 21.50, 27.50)
                )
            )
        ).flatten()

        val products = nestleShowcaseProducts + listOf(
            // Banana Philippines 1kg — best at LuLu (4.75), compared across Carrefour, Nesto, Viva
            demoProd(
                id = "demo_prod_banana_lulu",
                masterId = "master_banana_ph_1kg",
                retailerId = "demo_ret_lulu",
                flyerId = "demo_flyer_lulu",
                name = "Banana Philippines 1kg",
                normName = "banana philippines 1kg",
                brand = "Fresh Produce",
                category = "Fresh Food",
                barcode = "629100000101",
                imageUrl = bananaImg,
                price = 4.75,
                oldPrice = 5.95,
                unitSize = 1.0,
                unitType = "kg",
                baseQty = 1000.0,
                baseUnit = "g"
            ),
            demoProd(
                id = "demo_prod_banana_crf",
                masterId = "master_banana_ph_1kg",
                retailerId = "demo_ret_carrefour",
                flyerId = "demo_flyer_carrefour",
                name = "Banana Philippines 1kg",
                normName = "banana philippines 1kg",
                brand = "Fresh Produce",
                category = "Fresh Food",
                barcode = "629100000101",
                imageUrl = bananaImg,
                price = 5.25,
                oldPrice = 5.95,
                unitSize = 1.0,
                unitType = "kg",
                baseQty = 1000.0,
                baseUnit = "g"
            ),
            demoProd(
                id = "demo_prod_banana_nesto",
                masterId = "master_banana_ph_1kg",
                retailerId = "demo_ret_nesto",
                flyerId = "demo_flyer_nesto",
                name = "Banana Philippines 1kg",
                normName = "banana philippines 1kg",
                brand = "Fresh Produce",
                category = "Fresh Food",
                barcode = "629100000101",
                imageUrl = bananaImg,
                price = 4.95,
                oldPrice = 5.75,
                unitSize = 1.0,
                unitType = "kg",
                baseQty = 1000.0,
                baseUnit = "g"
            ),
            demoProd(
                id = "demo_prod_banana_viva",
                masterId = "master_banana_ph_1kg",
                retailerId = "demo_ret_viva",
                flyerId = "demo_flyer_viva",
                name = "Banana Philippines 1kg",
                normName = "banana philippines 1kg",
                brand = "Fresh Produce",
                category = "Fresh Food",
                barcode = "629100000101",
                imageUrl = bananaImg,
                price = 5.10,
                oldPrice = 5.90,
                unitSize = 1.0,
                unitType = "kg",
                baseQty = 1000.0,
                baseUnit = "g"
            ),

            // Noor Sunflower Oil 1.8L — best at Carrefour (16.90), compared across LuLu, Nesto
            demoProd(
                id = "demo_prod_noor_crf",
                masterId = "master_noor_oil_1_8l",
                retailerId = "demo_ret_carrefour",
                flyerId = "demo_flyer_carrefour",
                name = "Noor Sunflower Oil 1.8L",
                normName = "noor sunflower oil 1.8l",
                brand = "Noor",
                category = "Groceries",
                barcode = "629100000202",
                imageUrl = noorOilImg,
                price = 16.90,
                oldPrice = 19.90,
                unitSize = 1.8,
                unitType = "L",
                baseQty = 1800.0,
                baseUnit = "ml"
            ),
            demoProd(
                id = "demo_prod_noor_lulu",
                masterId = "master_noor_oil_1_8l",
                retailerId = "demo_ret_lulu",
                flyerId = "demo_flyer_lulu",
                name = "Noor Sunflower Oil 1.8L",
                normName = "noor sunflower oil 1.8l",
                brand = "Noor",
                category = "Groceries",
                barcode = "629100000202",
                imageUrl = noorOilImg,
                price = 17.50,
                oldPrice = 19.90,
                unitSize = 1.8,
                unitType = "L",
                baseQty = 1800.0,
                baseUnit = "ml"
            ),
            demoProd(
                id = "demo_prod_noor_nesto",
                masterId = "master_noor_oil_1_8l",
                retailerId = "demo_ret_nesto",
                flyerId = "demo_flyer_nesto",
                name = "Noor Sunflower Oil 1.8L",
                normName = "noor sunflower oil 1.8l",
                brand = "Noor",
                category = "Groceries",
                barcode = "629100000202",
                imageUrl = noorOilImg,
                price = 17.95,
                oldPrice = 20.50,
                unitSize = 1.8,
                unitType = "L",
                baseQty = 1800.0,
                baseUnit = "ml"
            ),

            // India Gate Basmati Rice 5kg — best at Nesto (34.90), compared across LuLu, Carrefour
            demoProd(
                id = "demo_prod_rice_nesto",
                masterId = "master_india_gate_5kg",
                retailerId = "demo_ret_nesto",
                flyerId = "demo_flyer_nesto",
                name = "India Gate Basmati Rice 5kg",
                normName = "india gate basmati rice 5kg",
                brand = "India Gate",
                category = "Groceries",
                barcode = "890100000303",
                imageUrl = basmatiImg,
                price = 34.90,
                oldPrice = 42.90,
                unitSize = 5.0,
                unitType = "kg",
                baseQty = 5000.0,
                baseUnit = "g"
            ),
            demoProd(
                id = "demo_prod_rice_lulu",
                masterId = "master_india_gate_5kg",
                retailerId = "demo_ret_lulu",
                flyerId = "demo_flyer_lulu",
                name = "India Gate Basmati Rice 5kg",
                normName = "india gate basmati rice 5kg",
                brand = "India Gate",
                category = "Groceries",
                barcode = "890100000303",
                imageUrl = basmatiImg,
                price = 36.75,
                oldPrice = 42.90,
                unitSize = 5.0,
                unitType = "kg",
                baseQty = 5000.0,
                baseUnit = "g"
            ),
            demoProd(
                id = "demo_prod_rice_crf",
                masterId = "master_india_gate_5kg",
                retailerId = "demo_ret_carrefour",
                flyerId = "demo_flyer_carrefour",
                name = "India Gate Basmati Rice 5kg",
                normName = "india gate basmati rice 5kg",
                brand = "India Gate",
                category = "Groceries",
                barcode = "890100000303",
                imageUrl = basmatiImg,
                price = 38.50,
                oldPrice = 42.90,
                unitSize = 5.0,
                unitType = "kg",
                baseQty = 5000.0,
                baseUnit = "g"
            ),

            // Al Ain Milk Full Cream 1L — best at Viva (3.95), compared across LuLu, Carrefour, Nesto
            demoProd(
                id = "demo_prod_milk_viva",
                masterId = "master_al_ain_milk_1l",
                retailerId = "demo_ret_viva",
                flyerId = "demo_flyer_viva",
                name = "Al Ain Milk Full Cream 1L",
                normName = "al ain milk full cream 1l",
                brand = "Al Ain",
                category = "Groceries",
                barcode = "629100000404",
                imageUrl = milkImg,
                price = 3.95,
                oldPrice = 4.40,
                unitSize = 1.0,
                unitType = "L",
                baseQty = 1000.0,
                baseUnit = "ml"
            ),
            demoProd(
                id = "demo_prod_milk_lulu",
                masterId = "master_al_ain_milk_1l",
                retailerId = "demo_ret_lulu",
                flyerId = "demo_flyer_lulu",
                name = "Al Ain Milk Full Cream 1L",
                normName = "al ain milk full cream 1l",
                brand = "Al Ain",
                category = "Groceries",
                barcode = "629100000404",
                imageUrl = milkImg,
                price = 4.20,
                oldPrice = 4.60,
                unitSize = 1.0,
                unitType = "L",
                baseQty = 1000.0,
                baseUnit = "ml"
            ),
            demoProd(
                id = "demo_prod_milk_crf",
                masterId = "master_al_ain_milk_1l",
                retailerId = "demo_ret_carrefour",
                flyerId = "demo_flyer_carrefour",
                name = "Al Ain Milk Full Cream 1L",
                normName = "al ain milk full cream 1l",
                brand = "Al Ain",
                category = "Groceries",
                barcode = "629100000404",
                imageUrl = milkImg,
                price = 4.35,
                oldPrice = 4.60,
                unitSize = 1.0,
                unitType = "L",
                baseQty = 1000.0,
                baseUnit = "ml"
            )
        )

        val promotions = listOf(
            PromotionEntity(
                id = "demo_promo_nestle",
                retailerId = "demo_ret_carrefour",
                title = "Nestlé & Nescafé Mega Week",
                description = "Up to 28% off Nescafé Gold, KitKat, Maggi noodles & Pure Life across UAE hypermarkets.",
                discount = 28.0,
                validFrom = now - dayMs,
                validTo = now + 5 * dayMs,
                status = "published",
                createdAt = now - dayMs,
                updatedAt = now,
                discountInfo = "UP TO 28% OFF"
            ),
            PromotionEntity(
                id = "demo_promo_weekend",
                retailerId = "demo_ret_lulu",
                title = "Weekend Fresh Produce Festival",
                description = "Up to 25% off tropical fruits, bananas and organic greens across LuLu UAE.",
                discount = 25.0,
                validFrom = now - dayMs,
                validTo = now + 3 * dayMs,
                status = "published",
                createdAt = now - dayMs,
                updatedAt = now,
                discountInfo = "UP TO 25% OFF"
            ),
            PromotionEntity(
                id = "demo_promo_pantry",
                retailerId = "demo_ret_carrefour",
                title = "UAE Pantry Bulk Super Savers",
                description = "Save on Noor oil, Basmati rice, and family packs at Carrefour with SHARE.",
                discount = 20.0,
                validFrom = now - dayMs,
                validTo = now + 5 * dayMs,
                status = "published",
                createdAt = now - dayMs,
                updatedAt = now,
                discountInfo = "SAVE 20%"
            ),
            PromotionEntity(
                id = "demo_promo_dairy",
                retailerId = "demo_ret_viva",
                title = "Morning Dairy & Breakfast Value",
                description = "Al Ain fresh milk and bakery essentials at everyday low prices.",
                discount = 15.0,
                validFrom = now - dayMs,
                validTo = now + 4 * dayMs,
                status = "published",
                createdAt = now - dayMs,
                updatedAt = now,
                discountInfo = "15% OFF"
            )
        )

        val coupons = listOf(
            CouponEntity(
                id = "demo_cp_noon",
                retailerId = "",
                title = "20% OFF noon Orders",
                code = "NOON20",
                discountInfo = "20% OFF",
                expiry = now + 14 * dayMs,
                status = "published",
                storeName = "noon",
                domain = "noon.com",
                category = "Electronics",
                description = "Online orders",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            CouponEntity(
                id = "demo_cp_amazon",
                retailerId = "",
                title = "15% OFF Amazon.ae",
                code = "SAVE15",
                discountInfo = "15% OFF",
                expiry = now + 10 * dayMs,
                status = "published",
                storeName = "amazon.ae",
                domain = "amazon.ae",
                category = "Electronics",
                description = "App & website",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            CouponEntity(
                id = "demo_cp_carrefour",
                retailerId = "demo_ret_carrefour",
                title = "10% OFF Carrefour Online",
                code = "CAR10",
                discountInfo = "10% OFF",
                expiry = now + 7 * dayMs,
                status = "published",
                storeName = "Carrefour",
                domain = "carrefouruae.com",
                category = "Supermarkets",
                description = "Online orders",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            CouponEntity(
                id = "demo_cp_lulu",
                retailerId = "demo_ret_lulu",
                title = "10% OFF LuLu Online",
                code = "LULU10",
                discountInfo = "10% OFF",
                expiry = now + 9 * dayMs,
                status = "published",
                storeName = "LuLu Hypermarket",
                domain = "luluhypermarket.com",
                category = "Supermarkets",
                description = "App & website",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            CouponEntity(
                id = "demo_cp_namshi",
                retailerId = "",
                title = "25% OFF Namshi Fashion",
                code = "STYLE25",
                discountInfo = "25% OFF",
                expiry = now + 12 * dayMs,
                status = "published",
                storeName = "Namshi",
                domain = "namshi.com",
                category = "Fashion",
                description = "Online orders",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            CouponEntity(
                id = "demo_cp_shein",
                retailerId = "",
                title = "20% OFF SHEIN App",
                code = "SHEIN20",
                discountInfo = "20% OFF",
                expiry = now + 11 * dayMs,
                status = "published",
                storeName = "SHEIN",
                domain = "shein.com",
                category = "Fashion",
                description = "App & website",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            CouponEntity(
                id = "demo_cp_nesto",
                retailerId = "demo_ret_nesto",
                title = "10% OFF Nesto Online",
                code = "NESTO10",
                discountInfo = "10% OFF",
                expiry = now + 6 * dayMs,
                status = "published",
                storeName = "Nesto",
                domain = "nestogroup.com",
                category = "Supermarkets",
                description = "Online orders",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            ),
            CouponEntity(
                id = "demo_cp_talabat",
                retailerId = "",
                title = "15% OFF talabat Mart",
                code = "TALA15",
                discountInfo = "15% OFF",
                expiry = now + 5 * dayMs,
                status = "published",
                storeName = "talabat",
                domain = "talabat.com",
                category = "Supermarkets",
                description = "App & website",
                isGlobal = true,
                isFeatured = true,
                createdAt = now,
                updatedAt = now
            )
        )

        val couponStyles = mapOf(
            "NOON20" to DemoCouponStyle(
                code = "NOON20",
                logoUri = resUri(R.drawable.ic_brand_noon_ring),
                secondaryLabel = "noon",
                bgTopHex = 0xFFFEF6E4,
                bgBottomHex = 0xFFFAF0D9,
                styleKey = "noon"
            ),
            "SAVE15" to DemoCouponStyle(
                code = "SAVE15",
                logoUri = resUri(R.drawable.ic_brand_amazon),
                secondaryLabel = ".ae",
                bgTopHex = 0xFFF3F6FE,
                bgBottomHex = 0xFFE9EFFC,
                styleKey = "amazon"
            ),
            "CAR10" to DemoCouponStyle(
                code = "CAR10",
                logoUri = carrefourLogo,
                secondaryLabel = "Carrefour",
                bgTopHex = 0xFFF0F6FE,
                bgBottomHex = 0xFFE5EFFB,
                styleKey = "carrefour"
            ),
            "LULU10" to DemoCouponStyle(
                code = "LULU10",
                logoUri = luluLogo,
                secondaryLabel = "Hypermarket",
                bgTopHex = 0xFFF2FAF1,
                bgBottomHex = 0xFFE5F3E6,
                styleKey = "lulu"
            ),
            "STYLE25" to DemoCouponStyle(
                code = "STYLE25",
                logoUri = "",
                secondaryLabel = "نمشي",
                bgTopHex = 0xFFF5F1FE,
                bgBottomHex = 0xFFEBE5FB,
                styleKey = "namshi"
            ),
            "SHEIN20" to DemoCouponStyle(
                code = "SHEIN20",
                logoUri = resUri(R.drawable.ic_brand_shein),
                secondaryLabel = "SHEIN",
                bgTopHex = 0xFFFDF2F4,
                bgBottomHex = 0xFFF9E6EA,
                styleKey = "shein"
            ),
            "NESTO10" to DemoCouponStyle(
                code = "NESTO10",
                logoUri = resUri(R.drawable.ic_brand_nesto_bag),
                secondaryLabel = "NESTO",
                bgTopHex = 0xFFF1F9F5,
                bgBottomHex = 0xFFE5F2EB,
                styleKey = "nesto"
            ),
            "TALA15" to DemoCouponStyle(
                code = "TALA15",
                logoUri = "",
                secondaryLabel = "talabat",
                bgTopHex = 0xFFFEF4EE,
                bgBottomHex = 0xFFFCEADF,
                styleKey = "talabat"
            )
        )

        val walletSeedCards = listOf(
            LoyaltyCardEntity(
                id = "default_lulu",
                storeName = "LuLu Happiness",
                cardNumber = "971508421250",
                barcodeFormat = "CODE_128",
                colorHex = 0xFF510786,
                note = "1250",
                createdAt = 1000L
            ),
            LoyaltyCardEntity(
                id = "default_nesto",
                storeName = "Nesto INAAM",
                cardNumber = "971508420800",
                barcodeFormat = "CODE_128",
                colorHex = 0xFF121F30,
                note = "800",
                createdAt = 2000L
            ),
            LoyaltyCardEntity(
                id = "default_carrefour",
                storeName = "Carrefour SHARE",
                cardNumber = "971508420400",
                barcodeFormat = "CODE_128",
                colorHex = 0xFF9A0229,
                note = "400",
                createdAt = 3000L
            )
        )

        val walletPresets = listOf(
            DemoWalletPreset(
                id = "preset_lulu",
                name = "LuLu Happiness",
                subtitle = "LuLu",
                defaultPoints = "1250",
                defaultCardNumber = "971508421250",
                logoResUri = luluLogo,
                stackedLogoResUri = resUri(R.drawable.ic_fig_logo_lulu_stacked),
                cardBgColorHex = 0xFF510786,
                secondaryBgColorHex = 0xFF7B16AA,
                accentColorHex = 0xFFF2BE22,
                badgeTitle = "HAPPINESS",
                badgeSubtitle = "LOYALTY REWARDED"
            ),
            DemoWalletPreset(
                id = "preset_nesto",
                name = "Nesto INAAM",
                subtitle = "Nesto",
                defaultPoints = "800",
                defaultCardNumber = "971508420800",
                logoResUri = nestoLogo,
                stackedLogoResUri = resUri(R.drawable.ic_fig_logo_nesto_card),
                cardBgColorHex = 0xFF121F30,
                secondaryBgColorHex = 0xFF0A245E,
                accentColorHex = 0xFFDFBE68,
                badgeTitle = "INAAM",
                badgeSubtitle = "REWARDS"
            ),
            DemoWalletPreset(
                id = "preset_carrefour",
                name = "Carrefour SHARE",
                subtitle = "Carrefour",
                defaultPoints = "400",
                defaultCardNumber = "971508420400",
                logoResUri = carrefourLogo,
                stackedLogoResUri = resUri(R.drawable.ic_fig_logo_carrefour_stacked),
                cardBgColorHex = 0xFF9A0229,
                secondaryBgColorHex = 0xFFD90429,
                accentColorHex = 0xFFFFFFFF,
                badgeTitle = "SHARE",
                badgeSubtitle = "MAJID AL FUTTAIM"
            ),
            DemoWalletPreset(
                id = "preset_vox",
                name = "VOX Cinemas SHARE",
                subtitle = "VOX\nCINEMAS",
                defaultPoints = "250",
                defaultCardNumber = "971508420250",
                logoResUri = resUri(R.drawable.ic_fig_logo_maf_blue),
                stackedLogoResUri = resUri(R.drawable.ic_fig_logo_maf_blue),
                cardBgColorHex = 0xFF0D47A1,
                secondaryBgColorHex = 0xFF1976D2,
                accentColorHex = 0xFFFFFFFF,
                badgeTitle = "VOX SHARE",
                badgeSubtitle = "MAJID AL FUTTAIM"
            ),
            DemoWalletPreset(
                id = "preset_dubaimall",
                name = "Dubai Mall",
                subtitle = "Dubai\nMALL",
                defaultPoints = "500",
                defaultCardNumber = "971508420500",
                logoResUri = "",
                stackedLogoResUri = null,
                cardBgColorHex = 0xFF2B2B3C,
                secondaryBgColorHex = 0xFF42425B,
                accentColorHex = 0xFFFFFFFF,
                badgeTitle = "EMAAR",
                badgeSubtitle = "U BY EMAAR"
            )
        )

        val notifications = listOf(
            NotificationEntity(
                id = "demo_notif_nestle",
                title = "Trending: Nestlé & Nescafé Offers",
                message = "Nescafé Classic 200g from AED 18.95 at Nesto & Nescafé Gold at AED 31.95 at Carrefour.",
                timestamp = now - 15 * 60_000L,
                isRead = false
            ),
            NotificationEntity(
                id = "demo_notif_1",
                title = "Price Drop: Banana Philippines 1kg",
                message = "Now AED 4.75 (-20%) at LuLu Hypermarket Al Barsha.",
                timestamp = now - 35 * 60_000L,
                isRead = false
            ),
            NotificationEntity(
                id = "demo_notif_2",
                title = "New Weekly Flyer: Carrefour",
                message = "Mid-Week Big Savings circular is live with 20 pages of offers.",
                timestamp = now - 3 * 3_600_000L,
                isRead = false
            ),
            NotificationEntity(
                id = "demo_notif_3",
                title = "Coupon Ending Soon: TALA15",
                message = "Save 15% on talabat Mart orders before offer expiry.",
                timestamp = now - dayMs,
                isRead = true
            )
        )

        val trendingSearches = listOf(
            "Nestlé",
            "Nescafé",
            "KitKat",
            "Maggi",
            "Pure Life",
            "Banana",
            "Noor Oil",
            "Basmati Rice",
            "Al Ain Milk"
        )

        val sampleShoppingItems = listOf(
            ShoppingListItem(
                id = "demo_list_banana",
                listId = "My Lists",
                productId = "demo_prod_banana_lulu",
                name = "Banana Philippines 1kg",
                targetPrice = 4.75,
                qty = 2,
                isSelected = true
            ),
            ShoppingListItem(
                id = "demo_list_oil",
                listId = "My Lists",
                productId = "demo_prod_noor_crf",
                name = "Noor Sunflower Oil 1.8L",
                targetPrice = 16.90,
                qty = 1,
                isSelected = true
            ),
            ShoppingListItem(
                id = "demo_list_rice",
                listId = "My Lists",
                productId = "demo_prod_rice_nesto",
                name = "India Gate Basmati Rice 5kg",
                targetPrice = 34.90,
                qty = 1,
                isSelected = true
            ),
            ShoppingListItem(
                id = "demo_list_milk",
                listId = "My Lists",
                productId = "demo_prod_milk_viva",
                name = "Al Ain Milk Full Cream 1L",
                targetPrice = 3.95,
                qty = 3,
                isSelected = true
            )
        )

        val clearanceBundles = listOf(
            ClearanceBundle(
                id = "demo_clr_kitkat_smarties",
                retailerId = "demo_ret_lulu",
                branchName = "Al Barsha",
                title = "KitKat & Smarties Sweet Celebration Combo",
                category = "Snacks & Sweets",
                items = listOf(
                    ClearanceBundleItem("Nestlé KitKat 4-Finger 12x41.5g", 2, 31.50, kitkatImg),
                    ClearanceBundleItem("Nestlé Smarties Sharing Box 130g", 2, 12.50, smartiesImg)
                ),
                originalTotalPrice = 88.00,
                bundleOfferPrice = 8.80,
                expiryDateMillis = now + 1 * dayMs + 6 * 3_600_000L,
                stockRemaining = 3,
                isDemo = true
            ),
            ClearanceBundle(
                id = "demo_clr_nescafe_barista",
                retailerId = "demo_ret_carrefour",
                branchName = "Mall of the Emirates",
                title = "Nescafé Gold, Classic & 3in1 Coffee Bundle",
                category = "Coffee & Beverages",
                items = listOf(
                    ClearanceBundleItem("Nescafé Gold Instant Coffee 200g", 1, 42.50, nescafeGoldImg),
                    ClearanceBundleItem("Nescafé Classic Instant Coffee 200g", 1, 26.50, nescafeClassicImg),
                    ClearanceBundleItem("Nescafé 3in1 Original 24x20g", 1, 21.90, nescafe3in1Img)
                ),
                originalTotalPrice = 90.90,
                bundleOfferPrice = 10.90,
                expiryDateMillis = now + 2 * dayMs + 4 * 3_600_000L,
                stockRemaining = 5,
                isDemo = true
            ),
            ClearanceBundle(
                id = "demo_clr_maggi_water",
                retailerId = "demo_ret_nesto",
                branchName = "Al Rigga, Deira",
                title = "Maggi Noodles & Pure Life Pantry Combo",
                category = "Pantry & Meals",
                items = listOf(
                    ClearanceBundleItem("Maggi 2-Minute Noodles 5x77g", 3, 9.50, maggiImg),
                    ClearanceBundleItem("Nestlé Pure Life Water 6x1.5L", 2, 8.95, pureLifeImg)
                ),
                originalTotalPrice = 46.40,
                bundleOfferPrice = 6.95,
                expiryDateMillis = now + 3 * dayMs,
                stockRemaining = 7,
                isDemo = true
            ),
            ClearanceBundle(
                id = "demo_clr_iced_sparkling",
                retailerId = "demo_ret_viva",
                branchName = "JVC District 13",
                title = "Nescafé Iced Latte & S.Pellegrino Chilled Pack",
                category = "Coffee & Beverages",
                items = listOf(
                    ClearanceBundleItem("Nescafé Iced Latte Can 240ml", 4, 7.50, nescafeIcedLatteImg),
                    ClearanceBundleItem("S.Pellegrino Sparkling Water 500ml", 4, 6.25, spellegrinoImg)
                ),
                originalTotalPrice = 55.00,
                bundleOfferPrice = 11.00,
                expiryDateMillis = now + 4 * dayMs,
                stockRemaining = 6,
                isDemo = true
            ),
            ClearanceBundle(
                id = "demo_clr_family_breakfast",
                retailerId = "demo_ret_lulu",
                branchName = "Al Barsha",
                title = "Nestlé Family Breakfast & Snack Saver",
                category = "Pantry & Meals",
                items = listOf(
                    ClearanceBundleItem("Nescafé Gold Instant Coffee 200g", 1, 42.50, nescafeGoldImg),
                    ClearanceBundleItem("Nestlé KitKat 4-Finger 12x41.5g", 1, 31.50, kitkatImg),
                    ClearanceBundleItem("Maggi 2-Minute Noodles 5x77g", 2, 9.50, maggiImg)
                ),
                originalTotalPrice = 93.00,
                bundleOfferPrice = 23.25,
                expiryDateMillis = now + 5 * dayMs,
                stockRemaining = 4,
                isDemo = true
            ),
            ClearanceBundle(
                id = "demo_clr_friskies_water",
                retailerId = "demo_ret_carrefour",
                branchName = "Mall of the Emirates",
                title = "Purina Friskies & Pure Life Home Bundle",
                category = "Pet & Home",
                items = listOf(
                    ClearanceBundleItem("Purina Friskies Cat Food 1.5kg", 2, 27.50, friskiesImg),
                    ClearanceBundleItem("Nestlé Pure Life Water 6x1.5L", 2, 8.95, pureLifeImg)
                ),
                originalTotalPrice = 72.90,
                bundleOfferPrice = 21.85,
                expiryDateMillis = now + 6 * dayMs,
                stockRemaining = 8,
                isDemo = true
            )
        )

        return DemoCatalogSnapshot(
            retailers = retailers,
            branches = branches,
            flyers = flyers,
            products = products,
            promotions = promotions,
            coupons = coupons,
            couponStylesByCode = couponStyles,
            walletSeedCards = walletSeedCards,
            walletPresets = walletPresets,
            notifications = notifications,
            trendingSearches = trendingSearches,
            sampleShoppingItems = sampleShoppingItems,
            clearanceBundles = clearanceBundles
        )
    }
}
