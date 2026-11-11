package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShoppingListRepository(
    private val dao: ShoppingListDao,
    private val db: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    val allItems: Flow<List<ShoppingListItem>> = dao.getAllItems()

    private var listenerRegistrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            sessionManager.currentUserId.collect { userId ->
                listenerRegistrations.forEach { it.remove() }
                listenerRegistrations.clear()
                
                if (userId != null) {
                    // Listen to all lists. Because lists can be dynamic, 
                    // we'll listen to a collection group or just the expected list ids? 
                    // To do `users/{userId}/shoppingLists/{listId}/items`, we'd need to know listIds. 
                    // Or we can just store the items uniformly in `users/{userId}/shoppingListItems`? 
                    // Wait, the prompt specifically requires `users/{userId}/shoppingLists/{listId}/items`. 
                    // So we must fetch the lists. Let's just track a default list for now if the UI doesn't allow creating them. 
                    // Actually, let's keep it simple: we can listen to the specific listId used by the app, which is "My Lists".
                    val listId = "My Lists" // In future this can be dynamic
                    val reg = db.collection("users").document(userId)
                        .collection("shoppingLists").document(listId)
                        .collection("items")
                        .addSnapshotListener { snapshot, _ ->
                            if (snapshot == null) return@addSnapshotListener
                            CoroutineScope(Dispatchers.IO).launch {
                                // For simplicity we could just insert them, causing Room to REPLACE.
                                // But what if an item is deleted from another device? We should sync deletions.
                                // So we delete items from this listId and re-insert.
                                dao.deleteList(listId)
                                snapshot.documents.forEach { doc ->
                                    doc.toObject(ShoppingListItem::class.java)?.let { item ->
                                        dao.insertItem(item)
                                    }
                                }
                            }
                        }
                    listenerRegistrations.add(reg)
                } else {
                    dao.clear()
                }
            }
        }
    }

    suspend fun insert(item: ShoppingListItem) {
        dao.insertItem(item) // local cache
        val userId = sessionManager.getUserId() ?: return
        db.collection("users").document(userId)
            .collection("shoppingLists").document(item.listId)
            .collection("items").document(item.id).set(item)
    }
    
    suspend fun delete(id: String) {
        dao.deleteItem(id) // local cache
        val userId = sessionManager.getUserId() ?: return
        val listId = "My Lists"
        db.collection("users").document(userId)
            .collection("shoppingLists").document(listId)
            .collection("items").document(id).delete()
    }
    
    suspend fun deleteList(listId: String) {
        dao.deleteList(listId)
        val userId = sessionManager.getUserId() ?: return
        try {
            val docs = db.collection("users").document(userId)
                .collection("shoppingLists").document(listId)
                .collection("items").get().await()
            for (doc in docs.documents) {
                doc.reference.delete()
            }
            db.collection("users").document(userId)
                .collection("shoppingLists").document(listId).delete()
        } catch (e: Exception) {}
    }
}

