package com.greenify.greenifykt

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : ComponentActivity() {
    private fun profilePrefs() = getSharedPreferences("greenify_profile", MODE_PRIVATE)
    private val storage by lazy { FirebaseStorage.getInstance() }

    private fun uploadAvatarImage(uri: Uri, userId: String, onDone: (String?, String?) -> Unit) {
        val imageRef = storage.reference
            .child("profile_avatars")
            .child(userId)
            .child("${System.currentTimeMillis()}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { download -> onDone(download.toString(), null) }
                    .addOnFailureListener { err -> onDone(null, err.localizedMessage) }
            }
            .addOnFailureListener { err -> onDone(null, err.localizedMessage) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val prefs = profilePrefs()

        setContent {
            var fullName by rememberSaveable {
                mutableStateOf(
                    prefs.getString("full_name", user?.displayName ?: user?.email?.substringBefore("@") ?: "") ?: ""
                )
            }
            var location by rememberSaveable { mutableStateOf(prefs.getString("location", "") ?: "") }
            var bio by rememberSaveable { mutableStateOf(prefs.getString("bio", "") ?: "") }
            var avatarUrl by rememberSaveable { mutableStateOf(prefs.getString("avatar_url", "") ?: "") }
            var selectedAvatarUriText by rememberSaveable { mutableStateOf("") }
            var status by rememberSaveable { mutableStateOf("") }
            val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                selectedAvatarUriText = uri?.toString().orEmpty()
                if (uri != null) {
                    status = "Photo selected. Tap Save Profile."
                }
            }

            val email = user?.email ?: "No email"
            val uid = user?.uid ?: "Not signed in"

            GreenifyCalculatorTheme(darkTheme = ThemeModeManager.isDarkModeEnabled(this)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProfileScreen(
                        fullName = fullName,
                        location = location,
                        bio = bio,
                        avatarUrl = avatarUrl,
                        selectedAvatarUriText = selectedAvatarUriText,
                        email = email,
                        uid = uid,
                        status = status,
                        onNameChange = { fullName = it },
                        onLocationChange = { location = it },
                        onBioChange = { bio = it },
                        onPickAvatar = { avatarPicker.launch("image/*") },
                        onRemoveAvatar = {
                            selectedAvatarUriText = ""
                            avatarUrl = ""
                            status = "Photo removed."
                        },
                        onSave = {
                            val selectedUri = selectedAvatarUriText.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                            val accountId = user?.uid ?: "anonymous"

                            fun persistProfile(finalAvatarUrl: String) {
                                avatarUrl = finalAvatarUrl
                                prefs.edit()
                                    .putString("full_name", fullName.trim())
                                    .putString("location", location.trim())
                                    .putString("bio", bio.trim())
                                    .putString("avatar_url", finalAvatarUrl)
                                    .apply()
                                selectedAvatarUriText = ""
                                status = "Profile saved."
                            }

                            if (selectedUri != null) {
                                status = "Uploading profile photo..."
                                uploadAvatarImage(selectedUri, accountId) { downloadUrl, error ->
                                    if (error != null || downloadUrl.isNullOrBlank()) {
                                        status = "Save failed: ${error ?: "image upload error"}"
                                        return@uploadAvatarImage
                                    }
                                    persistProfile(downloadUrl)
                                }
                            } else {
                                persistProfile(avatarUrl.trim())
                            }
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    fullName: String,
    location: String,
    bio: String,
    avatarUrl: String,
    selectedAvatarUriText: String,
    email: String,
    uid: String,
    status: String,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Profile",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val previewUri = selectedAvatarUriText.takeIf { it.isNotBlank() } ?: avatarUrl
                if (previewUri.isNotBlank()) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = "Profile image",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8EDF6)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Add an image URL to preview your profile photo.", style = MaterialTheme.typography.bodySmall)
                }

                Text(email, style = MaterialTheme.typography.bodySmall)
                Text("UID: $uid", style = MaterialTheme.typography.labelSmall)
                Button(onClick = onPickAvatar, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose Photo From Device")
                }
                TextButton(onClick = onRemoveAvatar) {
                    Text("Remove Photo")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onNameChange,
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = onBioChange,
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text("Save Profile")
                }

                if (status.isNotBlank()) {
                    Text(status, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
