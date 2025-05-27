package com.example.edutech.naivgation

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class AuthService {
    private val auth = FirebaseAuth.getInstance()

    fun loginUser(
        email: String,
        password: String,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Please enter both email and password.")
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess("Login successful.")
                } else {
                    onError(task.exception?.localizedMessage ?: "Error during login.")
                }
            }
    }

    fun registerUser(
        email: String,
        password: String,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            onError("Please enter both email and password.")
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess("Registration successful.")
                } else {
                    onError(task.exception?.localizedMessage ?: "Error during registration.")
                }
            }
    }

    fun resetPassword(
        email: String,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        if (email.isBlank()) {
            onError("Please enter your email to reset password.")
            return
        }


        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthService", "Password reset email sent to $email")
                    onSuccess("Password reset email sent to $email. Check your inbox.")
                } else {
                    val errorMessage =
                        task.exception?.localizedMessage ?: "Error sending password reset email."
                    Log.e(
                        "AuthService",
                        "Error sending password reset email: $errorMessage",
                        task.exception
                    )
                    onError(errorMessage)
                }
            }
    }

}