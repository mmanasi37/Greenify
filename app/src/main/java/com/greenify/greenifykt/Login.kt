package com.greenify.greenifykt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

class Login : ComponentActivity() {
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        if (mAuth.currentUser != null) {
            startActivity(Intent(applicationContext, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen(
                        darkMode = darkMode,
                        onToggleTheme = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onLogin = { email, password, onLoadingChange, onError ->
                            onLoadingChange(true)
                            mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    onLoadingChange(false)
                                    if (task.isSuccessful) {
                                        Toast.makeText(this@Login, "Authentication Successful.", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(applicationContext, MainActivity::class.java))
                                        finish()
                                    } else {
                                        onError(task.exception?.localizedMessage ?: "Authentication failed.")
                                    }
                                }
                        },
                        onOpenRegister = {
                            startActivity(Intent(applicationContext, Registration::class.java))
                            finish()
                        },
                        onOpenForgotPassword = {
                            startActivity(Intent(applicationContext, ForgotPassword::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    darkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onLogin: (String, String, (Boolean) -> Unit, (String) -> Unit) -> Unit,
    onOpenRegister: () -> Unit,
    onOpenForgotPassword: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = if (darkMode) {
            listOf(Color(0xFF0F1F2A), Color(0xFF173549), Color(0xFF1E4D66))
        } else {
            listOf(Color(0xFF2B7A4B), Color(0xFF56A36A), Color(0xFF9FD8A6))
        }
    )
    val headingColor = if (darkMode) Color(0xFFE4F2FF) else Color(0xFFFFFFFF)
    val subtitleColor = if (darkMode) Color(0xFFC6DBE8) else Color(0xFFEAF6EE)
    val cardColor = if (darkMode) Color(0xFF16222B) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(18.dp)
    ) {
        TextButton(
            onClick = { onToggleTheme(!darkMode) },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(if (darkMode) "Light Mode" else "Dark Mode", color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = headingColor
            )
            Text(
                text = "Log in to continue your sustainability journey",
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = ""
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        enabled = !loading
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                        },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        enabled = !loading
                    )

                    TextButton(
                        onClick = { showPassword = !showPassword },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !loading
                    ) {
                        Text(if (showPassword) "Hide Password" else "Show Password")
                    }

                    if (errorMessage.isNotBlank()) {
                        Text(errorMessage, color = Color(0xFFB3261E), style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            val safeEmail = email.trim()
                            val safePassword = password.trim()

                            if (safeEmail.isBlank()) {
                                errorMessage = "Enter Email"
                                return@Button
                            }
                            if (safePassword.isBlank()) {
                                errorMessage = "Enter Password"
                                return@Button
                            }

                            onLogin(
                                safeEmail,
                                safePassword,
                                { loading = it },
                                { errorMessage = it }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.height(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Log In")
                        }
                    }

                    TextButton(onClick = onOpenForgotPassword, enabled = !loading) {
                        Text("Forgot Password?")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New here?")
                TextButton(onClick = onOpenRegister, enabled = !loading) {
                    Text("Create Account")
                }
            }
        }
    }
}