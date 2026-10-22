package com.group3.lostandfound.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.ResultState
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel managing item details view and resolution state in MVVM architecture.
 */
class DetailsViewModel(
    private val itemRepository: ItemRepository = ItemRepository()
) : ViewModel() {

    private val _itemState = MutableStateFlow<ItemReport?>(null)
    val itemState: StateFlow<ItemReport?> = _itemState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadItem(itemId: String) {
        if (itemId.isEmpty()) {
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            itemRepository.streamItemById(itemId)
                .catch {
                    _isLoading.value = false
                }
                .collect { report ->
                    _itemState.value = report
                    _isLoading.value = false
                }
        }
    }

    fun markAsResolved(itemId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = itemRepository.markItemResolved(itemId)
            onResult(result is ResultState.Success)
        }
    }
}
