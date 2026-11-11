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
                android.util.Log.e("FirebaseManager", "Mapping error", e)
            }
        }
    }
    awaitClose { listener.remove() }
}
