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
import com.google.firebase.firestore.FirebaseFirestore

class Registration : ComponentActivity() {
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RegistrationScreen(
                        darkMode = darkMode,
                        onToggleTheme = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onRegister = { email, password, firstName, lastName, onLoadingChange, onError ->
                            onLoadingChange(true)
                            mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    onLoadingChange(false)
                                    if (task.isSuccessful) {
                                        val uid = mAuth.currentUser?.uid
                                        if (uid != null) {
                                            val fullName = "$firstName $lastName".trim()
                                            db.collection("users").document(uid).set(
                                                mapOf(
                                                    "first_name" to firstName,
                                                    "last_name" to lastName,
                                                    "full_name" to fullName,
                                                    "email" to email
                                                )
                                            )
                                            getSharedPreferences("greenify_profile", MODE_PRIVATE).edit()
                                                .putString("first_name", firstName)
                                                .putString("last_name", lastName)
                                                .putString("full_name", fullName)
                                                .apply()
                                        }
                                        Toast.makeText(this@Registration, "Registration Successful", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(applicationContext, Login::class.java))
                                        finish()
                                    } else {
                                        onError(task.exception?.localizedMessage ?: "Authentication failed")
                                    }
                                }
                        },
                        onOpenLogin = {
                            startActivity(Intent(applicationContext, Login::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistrationScreen(
    darkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onRegister: (String, String, String, String, (Boolean) -> Unit, (String) -> Unit) -> Unit,
    onOpenLogin: () -> Unit
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = if (darkMode) {
            listOf(Color(0xFF10212D), Color(0xFF1A3950), Color(0xFF245E73))
        } else {
            listOf(Color(0xFF2A6D8A), Color(0xFF3D8FB0), Color(0xFF90C5D8))
        }
    )
    val headingColor = if (darkMode) Color(0xFFE6F4FF) else Color(0xFFFFFFFF)
    val subtitleColor = if (darkMode) Color(0xFFC7DDE8) else Color(0xFFEAF6FF)
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
                text = "Create Account",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = headingColor
            )
            Text(
                text = "Join Greenify and start tracking your impact",
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
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            errorMessage = ""
                        },
                        label = { Text("First Name") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        enabled = !loading
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it
                            errorMessage = ""
                        },
                        label = { Text("Last Name") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        enabled = !loading
                    )

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

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = ""
                        },
                        label = { Text("Confirm Password") },
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
                            val safeFirstName = firstName.trim()
                            val safeLastName = lastName.trim()
                            val safeEmail = email.trim()
                            val safePassword = password.trim()
                            val safeConfirmPassword = confirmPassword.trim()

                            if (safeFirstName.isBlank()) {
                                errorMessage = "Enter your first name"
                                return@Button
                            }
                            if (safeLastName.isBlank()) {
                                errorMessage = "Enter your last name"
                                return@Button
                            }
                            if (safeEmail.isBlank()) {
                                errorMessage = "Enter Email"
                                return@Button
                            }
                            if (safePassword.isBlank()) {
                                errorMessage = "Enter Password"
                                return@Button
                            }
                            if (safeConfirmPassword.isBlank()) {
                                errorMessage = "Confirm your password"
                                return@Button
                            }
                            if (safePassword != safeConfirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@Button
                            }

                            onRegister(
                                safeEmail,
                                safePassword,
                                safeFirstName,
                                safeLastName,
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
                            Text("Create Account")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already registered?", color = headingColor)
                TextButton(onClick = onOpenLogin, enabled = !loading) {
                    Text("Log In")
                }
            }
        }
    }
}
