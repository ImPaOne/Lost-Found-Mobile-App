package com.group3.lostandfound.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.model.ExploreUiState
import com.group3.lostandfound.repository.ExploreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ExploreViewModel(
    // Repository provides filtered report data.
    private val exploreRepository: ExploreRepository = ExploreRepository()
) : ViewModel() {

    // Holds the current UI state and allows the ViewModel to modify it.
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var filterJob: Job? = null

    init {
        applyFilter()
    }

    // Called when the user types in the search box.
    fun setSearchQuery(query: String) {
        if (_uiState.value.query == query) return
        _uiState.value = _uiState.value.copy(query = query)

        // Debounce text search query
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(250)
            applyFilter()
        }
    }
    //Called when user select All,Lost,Found
    fun setFilterStatus(status: String) {
        if (_uiState.value.filterStatus == status) return
        _uiState.value = _uiState.value.copy(filterStatus = status)
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            applyFilter()
        }
    }

    //Called when usered choose grid or list layout
    fun setViewMode(isGrid: Boolean) {
        _uiState.value = _uiState.value.copy(isGridMode = isGrid)
    }

    //// Retrieves items from the repository using the current search and filter.
    private fun applyFilter() {
        val currentQuery = _uiState.value.query
        val currentStatus = _uiState.value.filterStatus

        viewModelScope.launch {
            exploreRepository.getItemsFiltered(
                query = currentQuery,
                status = currentStatus
            ).catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to filter items."
                )
            }.collect { filteredList ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = filteredList,
                    errorMessage = null
                )
            }
        }
    }
}
