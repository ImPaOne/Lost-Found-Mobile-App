package com.group3.lostandfound.model

import com.group3.lostandfound.ConversationMetadata

/**
 * Common sealed and data classes representing UI State for MVVM architecture.
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val isAdmin: Boolean, val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val recentItems: List<ItemReport> = emptyList(),
    val urgentItems: List<ItemReport> = emptyList(),
    val userGreetingName: String = "Community Member",
    val errorMessage: String? = null
)

data class ExploreUiState(
    val isLoading: Boolean = true,
    val items: List<ItemReport> = emptyList(),
    val query: String = "",
    val filterStatus: String = "ALL",
    val isGridMode: Boolean = true,
    val errorMessage: String? = null
)

data class MessagesUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationMetadata> = emptyList(),
    val errorMessage: String? = null
)

data class ReportsUiState(
    val isLoading: Boolean = true,
    val reports: List<ItemReport> = emptyList(),
    val errorMessage: String? = null
)
