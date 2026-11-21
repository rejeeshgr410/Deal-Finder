package com.example.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DemoRegistry
import com.example.data.ShoppingListItem
import com.example.data.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListViewModel(private val repository: ShoppingListRepository) : ViewModel() {
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val listNames: StateFlow<List<String>> = repository.listNames
    val syncError: StateFlow<String?> = repository.syncError

    val uiState: StateFlow<List<ShoppingListItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.allItems.collect {
                _isLoaded.value = true
            }
        }
    }

    fun seedDemoItemsIfEmpty() {
        viewModelScope.launch {
            val existing = repository.allItems.first()
            if (existing.isEmpty() && DemoRegistry.isDemoEnabled.value) {
                val sampleItems = DemoRegistry.currentSnapshot().sampleShoppingItems
                sampleItems.forEach { item ->
                    repository.insert(item)
                }
            }
        }
    }

    fun createList(name: String) {
        repository.registerListName(name)
    }

    fun removeItem(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun removeList(listId: String) {
        viewModelScope.launch {
            repository.deleteList(listId)
        }
    }

    fun addItem(item: ShoppingListItem) {
        viewModelScope.launch {
            val current = repository.allItems.first()
            val duplicate = current.firstOrNull { existing ->
                existing.listId == item.listId && (
                    (item.productId.isNotBlank() && existing.productId == item.productId) ||
                        existing.name.trim().equals(item.name.trim(), ignoreCase = true)
                    )
            }
            if (duplicate != null) {
                repository.insert(
                    duplicate.copy(
                        qty = duplicate.qty + item.qty.coerceAtLeast(1),
                        targetPrice = if (item.targetPrice > 0.0) item.targetPrice else duplicate.targetPrice,
                        isSelected = true
                    )
                )
            } else {
                repository.insert(item)
            }
        }
    }

    fun updateItem(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun clearAllLocal() {
        viewModelScope.launch {
            repository.clearAllLocal()
        }
    }

    fun clearSyncError() {
        repository.clearSyncError()
    }
}
