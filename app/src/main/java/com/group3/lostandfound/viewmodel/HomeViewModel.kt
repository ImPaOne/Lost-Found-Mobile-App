package com.group3.lostandfound.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.model.HomeUiState
import com.group3.lostandfound.model.ItemReport
import com.group3.lostandfound.repository.AuthRepository
import com.group3.lostandfound.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for Home dashboard in MVVM architecture.
 * Combines real-time items stream and user profile stream into HomeUiState.
 */
class HomeViewModel(
    private val itemRepository: ItemRepository = ItemRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        val uid = authRepository.currentUid

        viewModelScope.launch {
            combine(
                itemRepository.streamAllItems(),
                authRepository.getUserDisplayName(uid)
            ) { allItems, userName ->
                val urgentList = allItems.filter { item ->
                    item.status.equals("LOST", ignoreCase = true) ||
                    item.category.contains("Electronics", ignoreCase = true) ||
                    item.category.contains("Document", ignoreCase = true)
                }.take(5)

                HomeUiState(
                    isLoading = false,
                    recentItems = allItems,
                    urgentItems = urgentList,
                    userGreetingName = userName,
                    errorMessage = null
                )
            }.catch { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.localizedMessage ?: "Failed to load dashboard data."
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
