package com.group3.lostandfound.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.model.MessagesUiState
import com.group3.lostandfound.repository.AuthRepository
import com.group3.lostandfound.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel managing user conversations stream in MVVM architecture.
 */
class MessagesViewModel(
    private val messagesRepository: MessagesRepository = MessagesRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        observeConversations()
    }

    fun observeConversations() {
        val uid = authRepository.currentUid
        if (uid.isEmpty()) {
            _uiState.value = MessagesUiState(isLoading = false, conversations = emptyList())
            return
        }

        viewModelScope.launch {
            messagesRepository.streamUserConversations(uid)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load conversations."
                    )
                }
                .collect { list ->
                    _uiState.value = MessagesUiState(
                        isLoading = false,
                        conversations = list,
                        errorMessage = null
                    )
                }
        }
    }
}
