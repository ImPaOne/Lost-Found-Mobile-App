package com.group3.lostandfound.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.model.ReportsUiState
import com.group3.lostandfound.repository.AuthRepository
import com.group3.lostandfound.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel managing user's created reports in MVVM architecture.
 */
class ReportsViewModel(
    private val itemRepository: ItemRepository = ItemRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadUserReports()
    }

    fun loadUserReports() {
        val uid = authRepository.currentUid
        if (uid.isEmpty()) {
            _uiState.value = ReportsUiState(isLoading = false, reports = emptyList())
            return
        }

        viewModelScope.launch {
            itemRepository.streamUserReports(uid)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load reports."
                    )
                }
                .collect { list ->
                    _uiState.value = ReportsUiState(
                        isLoading = false,
                        reports = list,
                        errorMessage = null
                    )
                }
        }
    }

    fun markItemResolved(itemId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = itemRepository.markItemResolved(itemId)
            onComplete(result is com.group3.lostandfound.ResultState.Success)
        }
    }

    fun deleteItem(itemId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = itemRepository.deleteItem(itemId)
            onComplete(result is com.group3.lostandfound.ResultState.Success)
        }
    }
}
