package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

inline fun <reified T> Query.asFlow(): Flow<List<T>> = callbackFlow {
    val listener = addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
            android.util.Log.e("FirebaseManager", "Firestore error", error)
            // offline, don't close.
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
                        } catch (docErr: Exception) {
                            if (T::class == ProductEntity::class) {
                                val d = doc.data ?: return@mapNotNull null
                                ProductEntity(
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
                                    currentPrice = (d["currentPrice"] as? Number)?.toDouble() ?: 0.0,
                                    oldPrice = (d["oldPrice"] as? Number)?.toDouble(),
                                    unitSize = (d["unitSize"] as? Number)?.toDouble() ?: 1.0,
                                    unitType = d["unitType"] as? String ?: "unit",
                                    validFrom = (d["validFrom"] as? Number)?.toLong() ?: 0L,
                                    validTo = (d["validTo"] as? Number)?.toLong() ?: 0L,
                                    status = d["status"] as? String ?: "published",
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
                                ) as? T
                            } else if (T::class == BranchEntity::class) {
                                val d = doc.data ?: return@mapNotNull null
                                BranchEntity(
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
                                ) as? T
                            } else {
                                null
                            }
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
