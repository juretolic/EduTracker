package com.example.edutech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edutech.naivgation.AuthService
import com.example.edutech.ui.theme.EduTechTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.FirebaseApp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    // 1) launcher za rezultat Google Sign-In Intenta
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 2) registriraj launcher prije setContent
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    // kreiraj credential i proslijedi ga u "pravu" funkciju
                    val credential = GoogleAuthProvider
                        .getCredential(account.idToken!!, null)
                    signInOrRegisterWithGoogle(credential)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign-in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setContent {
            EduTechTheme {
                AuthenticationScreen(
                    googleSignInClient = googleSignInClient,
                    auth = auth,
                    signInWithGoogle = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                    onAuthSuccess = {
                        startActivity(
                            Intent(this, HomeActivity::class.java)
                                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
                        )
                        finish()
                    }
                )
            }
        }
    }

    // 4) tvoja "prava" funkcija kojoj se prosljeđuje AuthCredential
    private fun signInOrRegisterWithGoogle(credential: AuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                Log.d("GoogleSignIn", "Sign-in successful")
                Toast.makeText(this, "Google sign-in successful", Toast.LENGTH_SHORT).show()
                // nakon uspjeha radiš istu navigaciju
                startActivity(
                    Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
            } else {
                val errorMessage = authTask.exception?.localizedMessage ?: "Unknown error"
                Log.e("GoogleSignIn", "Sign-in failed: $errorMessage", authTask.exception)
                Toast.makeText(this, "Google sign-in failed: $errorMessage", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    googleSignInClient: GoogleSignInClient, auth: FirebaseAuth, signInWithGoogle: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val authService = remember { AuthService() }

    val annotatedLinkString = buildAuthSwitchAnnotatedString(isRegister)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EduTracker", style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF1E88E5)
                    ), textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))



                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        loading = true
                        if (isRegister) {
                            authService.registerUser(email, password, { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                                loading = false
                            }, { success ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(success)
                                }
                                loading = false
                                onAuthSuccess()
                            })
                        } else {
                            authService.loginUser(email, password, { error ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                                loading = false
                            }, { success ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(success)
                                }
                                loading = false
                                onAuthSuccess()
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegister) "Register" else "Login",
                            color = Color.White,
                            fontSize = 16.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))


                if(!isRegister) GoogleSignInButton {
                    signInWithGoogle()
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = annotatedLinkString,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                        modifier = Modifier.clickable {
                            isRegister = !isRegister // Toggle state on click
                        })

                }

                Spacer(modifier = Modifier.height(12.dp)) // Add some space
            }



            if (!isRegister) Text(
                text = "Forgot Password?",
                style = MaterialTheme.typography.bodySmall, // Use a smaller style
                modifier = Modifier
                    .align(Alignment.End) // Align to the right
                    .clickable {
                        authService.resetPassword(email, onError = { error ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        }, onSuccess = { success ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(success)
                            }
                        })
                    })
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }


}


@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1E88E5)
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google Sign-In",
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Prijava putem Google računa",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun buildAuthSwitchAnnotatedString(isRegister: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val text = if (isRegister) "Already have an account? Sign in" else "Don't have an account? Register"
        append(text)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            ),
            start = text.indexOf(if (isRegister) "Sign in" else "Register"),
            end = text.length
        )
        addStringAnnotation(
            tag = "auth_switch",
            annotation = "auth_switch",
            start = text.indexOf(if (isRegister) "Sign in" else "Register"),
            end = text.length
        )
    }
}