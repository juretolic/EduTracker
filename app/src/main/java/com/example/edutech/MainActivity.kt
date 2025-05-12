package com.example.edutech

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.TextFieldValue
import com.example.edutech.ui.theme.EduTechTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            EduTechTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthenticationScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AuthenticationScreen(modifier: Modifier = Modifier) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = if (isRegister) "Register" else "Login", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        if (successMessage.isNotEmpty()) {
            Text(text = successMessage, color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                loading = true // Start loading
                if (isRegister) {
                    registerUser(email.text, password.text, { error ->
                        errorMessage = error
                        loading = false // Stop loading on error
                    }, { success ->
                        successMessage = success
                        loading = false // Stop loading on success
                    })
                } else {
                    loginUser(email.text, password.text, { error ->
                        errorMessage = error
                        loading = false // Stop loading on error
                    }, { success ->
                        successMessage = success
                        loading = false // Stop loading on success
                    })
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading // Disable the button while loading
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = if (isRegister) "Register" else "Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isRegister = !isRegister }) {
            Text(text = if (isRegister) "Already have an account? Login" else "Don't have an account? Register")
        }
    }
}

fun loginUser(email: String, password: String, onError: (String) -> Unit, onSuccess: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess("Login Successful")
                // Navigate to the next screen or home page
            } else {
                onError(task.exception?.localizedMessage ?: "Login failed")
            }
        }
}

fun registerUser(email: String, password: String, onError: (String) -> Unit, onSuccess: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess("Registration Successful")
            } else {
                onError(task.exception?.localizedMessage ?: "Registration failed")
            }
        }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    EduTechTheme {
        AuthenticationScreen()
    }
}
