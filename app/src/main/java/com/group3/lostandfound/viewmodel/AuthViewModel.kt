package com.group3.lostandfound.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group3.lostandfound.ResultState
import com.group3.lostandfound.model.AuthUiState
import com.group3.lostandfound.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing authentication state for Login, SignUp, and Password Reset screens.
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val signUpState: StateFlow<AuthUiState> = _signUpState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val resetPasswordState: StateFlow<AuthUiState> = _resetPasswordState.asStateFlow()

    val isUserLoggedIn: Boolean
        get() = authRepository.isUserLoggedIn

    val currentUid: String
        get() = authRepository.currentUid

    fun login(email: String, password: String) {
        _loginState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signIn(email, password)) {
                is ResultState.Success -> {
                    _loginState.value = AuthUiState.Success(
                        isAdmin = result.data,
                        message = if (result.data) "Admin session verified." else "Welcome back."
                    )
                }
                is ResultState.Error -> {
                    _loginState.value = AuthUiState.Error(result.message)
                }
                is ResultState.Loading -> {
                    _loginState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun signUp(name: String, email: String, phone: String, nic: String, password: String) {
        _signUpState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signUp(name, email, phone, nic, password)) {
                is ResultState.Success -> {
                    _signUpState.value = AuthUiState.Success(
                        isAdmin = false,
                        message = "Account created successfully. Welcome to Foundly!"
                    )
                }
                is ResultState.Error -> {
                    _signUpState.value = AuthUiState.Error(result.message)
                }
                is ResultState.Loading -> {
                    _signUpState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        _resetPasswordState.value = AuthUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordReset(email)) {
                is ResultState.Success -> {
                    _resetPasswordState.value = AuthUiState.Success(
                        isAdmin = false,
                        message = "Password reset link sent to $email."
                    )
                }
                is ResultState.Error -> {
                    _resetPasswordState.value = AuthUiState.Error(result.message)
                }
                is ResultState.Loading -> {
                    _resetPasswordState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = AuthUiState.Idle
    }

    fun resetSignUpState() {
        _signUpState.value = AuthUiState.Idle
    }

    fun resetPasswordResetState() {
        _resetPasswordState.value = AuthUiState.Idle
    }
}
