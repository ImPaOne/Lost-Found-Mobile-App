
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
 * ViewModel for the Home dashboard.
 * Combines item data and user information into one UI state.
 */
class HomeViewModel(
    // Repositories provide item data and user authentication/profile data.
    private val itemRepository: ItemRepository = ItemRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    // Stores the current dashboard state and allows the ViewModel to update it.
    private val _uiState = MutableStateFlow(HomeUiState())

    // Exposes the state as read-only so the UI can observe it but cannot change it directly.
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Runs automatically when the ViewModel is created.
    init {
        loadHomeData()
    }

    fun loadHomeData() {
        // Gets the ID of the currently signed-in user.
        val uid = authRepository.currentUid

        // Launches a coroutine tied to the ViewModel lifecycle, avoiding blocking the UI thread
        // when the repository operations are implemented for asynchronous use.
        viewModelScope.launch {

            // Combines item updates and the user's display name into one stream.
            // The dashboard state is recalculated when either stream emits a new value.
            combine(
                itemRepository.streamAllItems(),
                authRepository.getUserDisplayName(uid)
            ) { allItems, userName ->

                // Selects lost items and items in important categories for the urgent section.
                val urgentList = allItems.filter { item ->
                    item.status.equals("LOST", ignoreCase = true) ||
                    item.category.contains("Electronics", ignoreCase = true) ||
                    item.category.contains("Document", ignoreCase = true)
                }.take(5) // Limits the urgent list to the first five matching items.

                // Creates the updated data the UI needs to display the dashboard.
                HomeUiState(
                    isLoading = false, // Data has been received, so loading is finished.
                    recentItems = allItems, // Supplies all items for the recent-items section.
                    urgentItems = urgentList, // Supplies filtered items for the urgent section.
                    userGreetingName = userName, // Supplies the name shown in the greeting.
                    errorMessage = null // Clears any previous error after successful data emission.
                )
            }

            // Handles errors from the combined flow and stores a message for the UI to display.
            .catch { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    // Uses the actual error message, or a fallback if no message is available.
                    errorMessage = exception.localizedMessage
                        ?: "Failed to load dashboard data."
                )
            }

            // Receives each new dashboard state and publishes it for the UI to observe.
            .collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
