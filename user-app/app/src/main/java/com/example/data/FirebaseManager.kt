package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.reflect.KClass

object FirebaseManager {
    // The shared backend uses a NAMED (non-default) Firestore database that was
    // provisioned by AI Studio. Both the consumer app and the business/admin web app
    // must read/write this exact database, otherwise their data will NOT be shared.
    const val FIRESTORE_DATABASE_ID = "ai-studio-86ad9cb9-2ca7-45ce-90e1-07624002c096"

    @Volatile
    private var firestore: FirebaseFirestore? = null

    /**
     * Initialises Firebase. The real project credentials come from `google-services.json`
     * (processed by the Google Services Gradle plugin), so we let Firebase auto-initialise
     * the default FirebaseApp instead of supplying hardcoded options.
     */
    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        // Eagerly build the named-database instance and apply settings once.
        getDb()
    }

    /**
     * Returns the Firestore instance bound to the shared named database. ALL Firestore
     * access in the app must go through this so every call targets the same database
     * as the business/admin app.
     */
    fun getDb(): FirebaseFirestore {
        return firestore ?: synchronized(this) {
            firestore ?: FirebaseFirestore.getInstance(FIRESTORE_DATABASE_ID).also { db ->
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
                    )
                    .build()
                db.firestoreSettings = settings
                firestore = db
            }
        }
    }
}

data class SnapshotEnvelope<T>(
    val items: List<T> = emptyList(),
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false,
    val lastServerSyncAtMs: Long? = null,
    val errorMessage: String? = null,
    val isTruncated: Boolean = false,
    val pageLimit: Int = 0
)

fun parseEpochMs(raw: Any?): Long {
    return when (raw) {
        is Number -> raw.toLong()
        is Timestamp -> raw.toDate().time
        is String -> {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return 0L
            trimmed.toLongOrNull()?.let { return it }
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd"
            )
            for (pattern in patterns) {
                try {
                    val fmt = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = false
                    }
                    val parsed = fmt.parse(trimmed)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                }
            }
            0L
        }
        else -> 0L
    }
}

private fun parseDoubleLenient(raw: Any?): Double {
    return when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.trim().toDoubleOrNull()
            ?: Regex("""(\d+(?:\.\d+)?)""").find(raw)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: 0.0
        else -> 0.0
    }
}

fun mapDocumentTolerantly(doc: DocumentSnapshot, clazz: KClass<*>): Any? {
    val d = doc.data ?: return null
    return when (clazz) {
        ProductEntity::class -> ProductEntity(
            id = doc.id,
            masterProductId = d["masterProductId"] as? String ?: "",
            retailerId = d["retailerId"] as? String ?: "",
            flyerId = d["flyerId"] as? String ?: "",
            name = d["name"] as? String ?: "",
            normalizedName = d["normalizedName"] as? String ?: "",
            searchKeywords = (d["searchKeywords"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            brand = d["brand"] as? String ?: "",
            category = d["category"] as? String ?: "General",
            barcode = d["barcode"] as? String ?: "",
            imageUrl = d["imageUrl"] as? String ?: "",
            currentPrice = parseDoubleLenient(d["currentPrice"]),
            oldPrice = (d["oldPrice"] as? Number)?.toDouble(),
            unitSize = (d["unitSize"] as? Number)?.toDouble() ?: 1.0,
            unitType = d["unitType"] as? String ?: "unit",
            validFrom = parseEpochMs(d["validFrom"]),
            validTo = parseEpochMs(d["validTo"]),
            status = d["status"] as? String ?: "published",
            createdAt = parseEpochMs(d["createdAt"]),
            updatedAt = parseEpochMs(d["updatedAt"]),
            schemaVersion = (d["schemaVersion"] as? Number)?.toInt() ?: 1,
            amountMinor = (d["amountMinor"] as? Number)?.toLong(),
            comparisonPriceMinor = (d["comparisonPriceMinor"] as? Number)?.toLong(),
            baseQuantity = (d["baseQuantity"] as? Number)?.toDouble(),
            baseUnit = d["baseUnit"] as? String,
            packCount = (d["packCount"] as? Number)?.toInt() ?: 1,
            minUnits = (d["minUnits"] as? Number)?.toInt() ?: 1,
            buyUnits = (d["buyUnits"] as? Number)?.toInt(),
            freeUnits = (d["freeUnits"] as? Number)?.toInt(),
            loyaltyRequired = d["loyaltyRequired"] as? Boolean ?: false,
            couponRequired = d["couponRequired"] as? Boolean ?: false,
            promoConditions = d["promoConditions"] as? String ?: "",
            availability = d["availability"] as? String ?: "unknown",
            sourceType = d["sourceType"] as? String ?: "",
            sourceLabel = d["sourceLabel"] as? String ?: "",
            verificationLabel = d["verificationLabel"] as? String ?: "unverified",
            lastVerifiedAt = (d["lastVerifiedAt"] as? Number)?.toLong(),
            isDemo = d["isDemo"] as? Boolean ?: false
        )
        BranchEntity::class -> BranchEntity(
            id = doc.id,
            retailerId = d["retailerId"] as? String ?: "",
            name = d["name"] as? String ?: "",
            emirate = d["emirate"] as? String ?: "",
            address = d["address"] as? String ?: "",
            latitude = (d["latitude"] as? Number)?.toDouble() ?: 0.0,
            longitude = (d["longitude"] as? Number)?.toDouble() ?: 0.0,
            phone = d["phone"] as? String ?: "",
            openingHours = d["openingHours"] as? String ?: "",
            coordsVerified = d["coordsVerified"] as? Boolean ?: false,
            coordSource = d["coordSource"] as? String ?: ""
        )
        PromotionEntity::class -> {
            val rawDiscount = d["discount"]
            val rawDiscountInfo = (d["discountInfo"] as? String).orEmpty()
            val resolvedDiscountInfo = rawDiscountInfo.ifBlank {
                (rawDiscount as? String).orEmpty()
            }
            PromotionEntity(
                id = doc.id,
                retailerId = d["retailerId"] as? String ?: "",
                title = d["title"] as? String ?: "",
                description = d["description"] as? String ?: "",
                discount = parseDoubleLenient(rawDiscount),
                validFrom = parseEpochMs(d["validFrom"]),
                validTo = parseEpochMs(d["validTo"]),
                status = d["status"] as? String ?: "published",
                createdAt = parseEpochMs(d["createdAt"]),
                updatedAt = parseEpochMs(d["updatedAt"]),
                discountInfo = resolvedDiscountInfo
            )
        }
        FlyerEntity::class -> FlyerEntity(
            id = doc.id,
            retailerId = d["retailerId"] as? String ?: "",
            title = d["title"] as? String ?: "",
            imageUrl = d["imageUrl"] as? String ?: "",
            pdfUrl = d["pdfUrl"] as? String ?: "",
            pages = (d["pages"] as? Number)?.toInt() ?: 0,
            status = d["status"] as? String ?: "published",
            searchKeywords = (d["searchKeywords"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            validFrom = parseEpochMs(d["validFrom"]),
            validTo = parseEpochMs(d["validTo"]),
            views = (d["views"] as? Number)?.toLong() ?: 0L,
            clicks = (d["clicks"] as? Number)?.toLong() ?: 0L,
            createdAt = parseEpochMs(d["createdAt"]),
            updatedAt = parseEpochMs(d["updatedAt"])
        )
        RetailerEntity::class -> {
            val emiratesVal = when (val rawEmirates = d["emirates"]) {
                is String -> rawEmirates
                is List<*> -> rawEmirates.mapNotNull { it as? String }.joinToString(", ")
                else -> ""
            }
            RetailerEntity(
                id = doc.id,
                name = d["name"] as? String ?: "",
                logoUrl = d["logoUrl"] as? String ?: "",
                status = d["status"] as? String ?: "published",
                emirates = emiratesVal,
                branchesCount = (d["branchesCount"] as? Number)?.toInt() ?: 0,
                searchKeywords = (d["searchKeywords"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                createdAt = parseEpochMs(d["createdAt"]),
                updatedAt = parseEpochMs(d["updatedAt"])
            )
        }
        CouponEntity::class -> CouponEntity(
            id = doc.id,
            retailerId = d["retailerId"] as? String ?: "",
            title = d["title"] as? String ?: "",
            code = d["code"] as? String ?: "",
            discountInfo = d["discountInfo"] as? String ?: "",
            expiry = parseEpochMs(d["expiry"]),
            status = d["status"] as? String ?: "published",
            storeName = d["storeName"] as? String ?: "",
            domain = d["domain"] as? String ?: "",
            category = d["category"] as? String ?: "",
            description = d["description"] as? String ?: "",
            isGlobal = d["isGlobal"] as? Boolean ?: false,
            isFeatured = d["isFeatured"] as? Boolean ?: false,
            createdAt = parseEpochMs(d["createdAt"]),
            updatedAt = parseEpochMs(d["updatedAt"])
        )
        else -> null
    }
}

inline fun <reified T> Query.asEnvelopeFlow(pageLimit: Int = 0): Flow<SnapshotEnvelope<T>> = callbackFlow {
    var lastServerSync: Long? = null
    val listener = addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
            android.util.Log.e("FirebaseManager", "Firestore error", error)
            trySend(
                SnapshotEnvelope(
                    items = emptyList(),
                    isFromCache = true,
                    lastServerSyncAtMs = lastServerSync,
                    errorMessage = error.localizedMessage ?: "Unable to reach server",
                    isTruncated = false,
                    pageLimit = pageLimit
                )
            )
        }
        if (snapshot != null) {
            val fromCache = snapshot.metadata.isFromCache
            val pendingWrites = snapshot.metadata.hasPendingWrites()
            if (!fromCache) {
                lastServerSync = System.currentTimeMillis()
            }
            val parsed: List<T> = try {
                snapshot.toObjects(T::class.java)
            } catch (e: Exception) {
                android.util.Log.w("FirebaseManager", "Standard mapping failed, attempting tolerant mapping for ${T::class.simpleName}: ${e.message}")
                try {
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(T::class.java)
                        } catch (_: Exception) {
                            mapDocumentTolerantly(doc, T::class) as? T
                        }
                    }
                } catch (fatal: Exception) {
                    android.util.Log.e("FirebaseManager", "Tolerant mapping also failed", fatal)
                    emptyList()
                }
            }
            trySend(
                SnapshotEnvelope(
                    items = parsed,
                    isFromCache = fromCache,
                    hasPendingWrites = pendingWrites,
                    lastServerSyncAtMs = lastServerSync,
                    errorMessage = error?.localizedMessage,
                    isTruncated = pageLimit > 0 && parsed.size >= pageLimit,
                    pageLimit = pageLimit
                )
            )
        }
    }
    awaitClose { listener.remove() }
}

inline fun <reified T> Query.asFlow(): Flow<List<T>> = callbackFlow {
    val listener = addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
            android.util.Log.e("FirebaseManager", "Firestore error", error)
        }
        if (snapshot != null) {
            try {
                trySend(snapshot.toObjects(T::class.java))
            } catch (e: Exception) {
                android.util.Log.w("FirebaseManager", "Standard mapping failed, attempting tolerant mapping for ${T::class.simpleName}: ${e.message}")
                try {
                    val parsed = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(T::class.java)
                        } catch (_: Exception) {
                            mapDocumentTolerantly(doc, T::class) as? T
                        }
                    }
                    trySend(parsed)
                } catch (fatal: Exception) {
                    android.util.Log.e("FirebaseManager", "Tolerant mapping also failed", fatal)
                }
            }
        }
    }
    awaitClose { listener.remove() }
}

inline fun <reified T> com.google.firebase.firestore.DocumentReference.asDocumentFlow(): Flow<T?> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            android.util.Log.e("FirebaseManager", "Firestore document error", error)
        }
        if (snapshot != null && snapshot.exists()) {
            try {
                trySend(snapshot.toObject(T::class.java))
            } catch (e: Exception) {
                android.util.Log.w("FirebaseManager", "Document mapping failed for ${T::class.simpleName}: ${e.message}")
            }
        } else if (snapshot != null) {
            trySend(null)
        }
    }
    awaitClose { listener.remove() }
}
