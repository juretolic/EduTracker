package com.example.edutech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutech.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        _authState.value = AuthState(user = authRepository.currentUser)
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            authRepository.signInWithEmailAndPassword(email, password)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        error = null
                    )
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Authentication failed"
                    )
                }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            authRepository.createUserWithEmailAndPassword(email, password)
                .onSuccess { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        user = user,
                        error = null
                    )
                }
                .onFailure { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = error.message ?: "User creation failed"
                    )
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState()
    }
} 