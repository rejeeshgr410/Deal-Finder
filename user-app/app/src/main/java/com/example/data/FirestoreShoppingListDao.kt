package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await

class FirestoreShoppingListDao(private val db: FirebaseFirestore, private val sessionManager: SessionManager) : ShoppingListDao {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getAllItems(): Flow<List<ShoppingListItem>> {
        return sessionManager.currentUserId.flatMapLatest { userId ->
            if (userId == null) kotlinx.coroutines.flow.emptyFlow()
            else db.collection("users").document(userId).collection("shopping_list").asFlow<ShoppingListItem>()
        }
    }

    override suspend fun insertItem(item: ShoppingListItem) {
        val userId = sessionManager.getUserId() ?: return
        val id = if (item.id.isEmpty() || item.id == "0") java.util.UUID.randomUUID().toString() else item.id
        val newItem = item.copy(id = id)
        try { db.collection("users").document(userId).collection("shopping_list").document(id).set(newItem).await() } catch (e: Exception) {}
    }

    override suspend fun deleteItem(id: String) {
        val userId = sessionManager.getUserId() ?: return
        try { db.collection("users").document(userId).collection("shopping_list").document(id).delete().await() } catch (e: Exception) {}
    }

    override suspend fun deleteList(listId: String) {
        val userId = sessionManager.getUserId() ?: return
        try {
            val docs = db.collection("users").document(userId).collection("shopping_list").whereEqualTo("listId", listId).get().await()
            for (doc in docs.documents) {
                try { doc.reference.delete().await() } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }

    override suspend fun clear() {
        val userId = sessionManager.getUserId() ?: return
        try {
            val docs = db.collection("users").document(userId).collection("shopping_list").get().await()
            for (doc in docs.documents) {
                try { doc.reference.delete().await() } catch (e: Exception) {}
            }
        } catch (e: Exception) {}
    }
}
