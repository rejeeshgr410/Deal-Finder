package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShoppingListRepository(
    private val dao: ShoppingListDao,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allItems: Flow<List<ShoppingListItem>> = dao.getAllItems()

    private val defaultLists = listOf("My Lists", "Grocery", "Ramadan", "Party")
    private val _listNames = MutableStateFlow(defaultLists)
    val listNames: StateFlow<List<String>> = _listNames.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private var listenerRegistrations = mutableListOf<ListenerRegistration>()

    init {
        repoScope.launch {
            combine(sessionManager.currentUserId, _listNames) { userId, names ->
                userId to names
            }.collect { (userId, names) ->
                listenerRegistrations.forEach { it.remove() }
                listenerRegistrations.clear()

                if (userId != null) {
                    // Discover any additional server-side lists
                    try {
                        val serverLists = db.collection("users").document(userId)
                            .collection("shoppingLists").get().await()
                        val discovered = serverLists.documents.mapNotNull { doc ->
                            doc.getString("name") ?: doc.id
                        }.filter { it.isNotBlank() }
                        val merged = (names + discovered).distinct()
                        if (merged != names) {
                            _listNames.value = merged
                            return@collect
                        }
                    } catch (_: Exception) {}

                    names.forEach { listId ->
                        val reg = db.collection("users").document(userId)
                            .collection("shoppingLists").document(listId)
                            .collection("items")
                            .addSnapshotListener { snapshot, err ->
                                if (err != null) {
                                    _syncError.value = err.localizedMessage
                                    return@addSnapshotListener
                                }
                                if (snapshot == null) return@addSnapshotListener
                                repoScope.launch {
                                    dao.deleteList(listId)
                                    snapshot.documents.forEach { doc ->
                                        doc.toObject(ShoppingListItem::class.java)?.let { item ->
                                            dao.insertItem(item.copy(listId = listId))
                                        }
                                    }
                                }
                            }
                        listenerRegistrations.add(reg)
                    }
                } else {
                    if (!DemoRegistry.isDemoEnabled.value) {
                        dao.clear()
                    }
                }
            }
        }
    }

    fun registerListName(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        if (clean !in _listNames.value) {
            _listNames.value = _listNames.value + clean
        }
    }

    fun unregisterListName(name: String) {
        _listNames.value = _listNames.value.filterNot { it == name }
    }

    suspend fun insert(item: ShoppingListItem) {
        registerListName(item.listId)
        dao.insertItem(item)
        val userId = sessionManager.getUserId() ?: return
        try {
            val listDoc = db.collection("users").document(userId)
                .collection("shoppingLists").document(item.listId)
            listDoc.set(mapOf("name" to item.listId, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            listDoc.collection("items").document(item.id).set(item).await()
            _syncError.value = null
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage
        }
    }

    suspend fun delete(id: String) {
        val existing = dao.getItemById(id)
        val actualListId = existing?.listId ?: "My Lists"
        dao.deleteItem(id)
        val userId = sessionManager.getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("shoppingLists").document(actualListId)
                .collection("items").document(id).delete().await()
            _syncError.value = null
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage
        }
    }

    suspend fun deleteList(listId: String) {
        unregisterListName(listId)
        dao.deleteList(listId)
        val userId = sessionManager.getUserId() ?: return
        try {
            val docs = db.collection("users").document(userId)
                .collection("shoppingLists").document(listId)
                .collection("items").get().await()
            for (doc in docs.documents) {
                doc.reference.delete().await()
            }
            db.collection("users").document(userId)
                .collection("shoppingLists").document(listId).delete().await()
            _syncError.value = null
        } catch (e: Exception) {
            _syncError.value = e.localizedMessage
        }
    }

    suspend fun clearAllLocal() {
        dao.clear()
        _listNames.value = defaultLists
    }

    fun clearSyncError() {
        _syncError.value = null
    }
}
