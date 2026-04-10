package com.greenify.greenifykt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class CommunityPost(
    val id: String,
    val userName: String,
    val userAvatarUrl: String,
    val message: String,
    val imageUrl: String,
    val co2SavedWeek: Double,
    val points: Int,
    val likes: Int,
    val createdAtMs: Long,
    val isMilestone: Boolean
)

private data class LeaderboardEntry(
    val userName: String,
    val points: Int,
    val streak: Int,
    val co2SavedWeek: Double
)

private data class MilestoneItem(
    val title: String,
    val subtitle: String
)

private data class CommunityStats(
    val points: Int,
    val streak: Int,
    val co2SavedWeek: Double,
    val userName: String,
    val userAvatarUrl: String,
    val userId: String?
)

class Community : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun challengePrefs() = getSharedPreferences("greenify_challenge", MODE_PRIVATE)
    private fun dashboardPrefs() = getSharedPreferences("greenify_dashboard", MODE_PRIVATE)
    private fun profilePrefs() = getSharedPreferences("greenify_profile", MODE_PRIVATE)

    private fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun todayKey(): String = dateFormatter.format(Date())

    private fun getStats(): CommunityStats {
        val points = challengePrefs().getInt("points", 0)
        val co2Bits = challengePrefs().getLong(
            "${currentWeekKey()}:co2_saved_bits",
            java.lang.Double.doubleToRawLongBits(0.0)
        )
        val co2SavedWeek = java.lang.Double.longBitsToDouble(co2Bits)

        val completedDates = dashboardPrefs().getStringSet("tip_completed_dates", emptySet()).orEmpty()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val weekStart = cal.time
        val streak = completedDates.count { dateText ->
            try {
                val date = dateFormatter.parse(dateText)
                date != null && !date.before(weekStart)
            } catch (_: Exception) {
                false
            }
        }

        val user = auth.currentUser
        val storedName = profilePrefs().getString("full_name", "")?.trim().orEmpty()
        val storedAvatar = profilePrefs().getString("avatar_url", "")?.trim().orEmpty()
        val userName = storedName.ifBlank {
            user?.displayName
                ?: user?.email?.substringBefore("@")
                ?: "Green User"
        }

        return CommunityStats(
            points = points,
            streak = streak,
            co2SavedWeek = co2SavedWeek,
            userName = userName,
            userAvatarUrl = storedAvatar,
            userId = user?.uid
        )
    }

    private fun buildMilestones(stats: CommunityStats): List<MilestoneItem> {
        val items = mutableListOf<MilestoneItem>()
        if (stats.points >= 100) items.add(MilestoneItem("Level Up", "Reached Level ${stats.points / 100 + 1}"))
        if (stats.co2SavedWeek >= 5.0) items.add(MilestoneItem("Impact Goal", "Saved ${String.format(Locale.US, "%.1f", stats.co2SavedWeek)} kg CO2 this week"))
        if (stats.streak >= 3) items.add(MilestoneItem("Consistency", "Completed eco actions on ${stats.streak} days this week"))
        if (items.isEmpty()) {
            items.add(MilestoneItem("Getting Started", "Complete challenges and tips to unlock milestones"))
        }
        return items
    }

    private fun upsertLeaderboardProfile(stats: CommunityStats) {
        val userId = stats.userId ?: return
        val payload = hashMapOf(
            "display_name" to stats.userName,
            "avatar_url" to stats.userAvatarUrl,
            "points" to stats.points,
            "streak" to stats.streak,
            "co2_saved_week" to stats.co2SavedWeek,
            "updated_at_ms" to System.currentTimeMillis()
        )
        db.collection("community_profiles").document(userId).set(payload)
    }

    private fun loadLeaderboard(stats: CommunityStats, onResult: (List<LeaderboardEntry>) -> Unit) {
        db.collection("community_profiles").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onResult(fallbackLeaderboard(stats))
                return@addOnCompleteListener
            }

            val snapshot = task.result as? QuerySnapshot
            val entries = snapshot?.documents
                ?.mapNotNull { doc ->
                    val name = doc.getString("display_name")?.trim().orEmpty()
                    val points = (doc.getLong("points") ?: 0L).toInt()
                    val streak = (doc.getLong("streak") ?: 0L).toInt()
                    val co2 = doc.getDouble("co2_saved_week") ?: 0.0
                    if (name.isBlank()) null else LeaderboardEntry(name, points, streak, co2)
                }
                .orEmpty()
                .sortedByDescending { it.points }

            onResult(if (entries.isNotEmpty()) entries.take(5) else fallbackLeaderboard(stats))
        }
    }

    private fun fallbackLeaderboard(stats: CommunityStats): List<LeaderboardEntry> {
        return listOf(
            LeaderboardEntry(stats.userName, stats.points, stats.streak, stats.co2SavedWeek),
            LeaderboardEntry("EcoRanger", 220, 5, 8.1),
            LeaderboardEntry("GreenPulse", 180, 4, 6.4)
        ).sortedByDescending { it.points }
    }

    private fun loadFeed(stats: CommunityStats, milestones: List<MilestoneItem>, onResult: (List<CommunityPost>) -> Unit) {
        db.collection("community_posts").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onResult(fallbackFeed(stats, milestones))
                return@addOnCompleteListener
            }

            val snapshot = task.result as? QuerySnapshot
            val posts = snapshot?.documents
                ?.mapNotNull { doc ->
                    val message = doc.getString("message")?.trim().orEmpty()
                    val userName = doc.getString("user_name")?.trim().orEmpty()
                    if (message.isBlank() || userName.isBlank()) return@mapNotNull null

                    CommunityPost(
                        id = doc.id,
                        userName = userName,
                        userAvatarUrl = doc.getString("author_avatar_url")?.trim().orEmpty(),
                        message = message,
                        imageUrl = doc.getString("image_url")?.trim().orEmpty(),
                        co2SavedWeek = doc.getDouble("co2_saved_week") ?: 0.0,
                        points = (doc.getLong("points") ?: 0L).toInt(),
                        likes = (doc.getLong("likes") ?: 0L).toInt(),
                        createdAtMs = doc.getLong("created_at_ms") ?: 0L,
                        isMilestone = doc.getBoolean("is_milestone") ?: false
                    )
                }
                .orEmpty()
                .sortedByDescending { it.createdAtMs }

            onResult(if (posts.isNotEmpty()) posts.take(20) else fallbackFeed(stats, milestones))
        }
    }

    private fun fallbackFeed(stats: CommunityStats, milestones: List<MilestoneItem>): List<CommunityPost> {
        val now = System.currentTimeMillis()
        val milestonePosts = milestones.mapIndexed { idx, item ->
            CommunityPost(
                id = "local_milestone_$idx",
                userName = stats.userName,
                userAvatarUrl = stats.userAvatarUrl,
                message = "${item.title}: ${item.subtitle}",
                imageUrl = "",
                co2SavedWeek = stats.co2SavedWeek,
                points = stats.points,
                likes = 0,
                createdAtMs = now - (idx * 1000L),
                isMilestone = true
            )
        }
        return listOf(
            CommunityPost("local_1", stats.userName, stats.userAvatarUrl, "I reduced my impact this week and kept up my eco streak.", "", stats.co2SavedWeek, stats.points, 0, now + 1000L, false)
        ) + milestonePosts
    }

    private fun publishPost(message: String, imageUrl: String, stats: CommunityStats, onDone: () -> Unit) {
        val payload = hashMapOf(
            "message" to message,
            "image_url" to imageUrl,
            "user_name" to stats.userName,
            "author_avatar_url" to stats.userAvatarUrl,
            "user_id" to (stats.userId ?: "anonymous"),
            "co2_saved_week" to stats.co2SavedWeek,
            "points" to stats.points,
            "likes" to 0,
            "is_milestone" to false,
            "created_at_ms" to System.currentTimeMillis()
        )

        db.collection("community_posts").add(payload).addOnCompleteListener { onDone() }
    }

    private fun uploadPostImage(imageUri: Uri, stats: CommunityStats, onDone: (String?, String?) -> Unit) {
        val userId = stats.userId ?: "anonymous"
        val imageRef = storage.reference
            .child("community_posts")
            .child(userId)
            .child("${System.currentTimeMillis()}.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri -> onDone(uri.toString(), null) }
                    .addOnFailureListener { err -> onDone(null, err.localizedMessage) }
            }
            .addOnFailureListener { err -> onDone(null, err.localizedMessage) }
    }

    private fun likePost(postId: String, onDone: () -> Unit) {
        if (postId.startsWith("local_")) {
            onDone()
            return
        }

        db.collection("community_posts").document(postId)
            .update("likes", FieldValue.increment(1))
            .addOnCompleteListener { onDone() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }
            var statusMessage by rememberSaveable { mutableStateOf("") }
            var postInput by rememberSaveable { mutableStateOf("") }
            var selectedImageUriText by rememberSaveable { mutableStateOf("") }
            var posts by remember { mutableStateOf(emptyList<CommunityPost>()) }
            var leaderboard by remember { mutableStateOf(emptyList<LeaderboardEntry>()) }
            var milestones by remember { mutableStateOf(emptyList<MilestoneItem>()) }
            var currentUserAvatarUrl by rememberSaveable { mutableStateOf("") }
            val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                selectedImageUriText = uri?.toString().orEmpty()
                if (uri != null) {
                    statusMessage = "Photo selected."
                }
            }

            fun refreshCommunity() {
                val stats = getStats()
                currentUserAvatarUrl = stats.userAvatarUrl
                upsertLeaderboardProfile(stats)
                val currentMilestones = buildMilestones(stats)
                milestones = currentMilestones

                loadLeaderboard(stats) { leaderboard = it }
                loadFeed(stats, currentMilestones) { posts = it }
            }

            LaunchedEffect(Unit) {
                refreshCommunity()
            }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CommunityScreen(
                        darkMode = darkMode,
                        statusMessage = statusMessage,
                        postInput = postInput,
                        selectedImageUriText = selectedImageUriText,
                        currentUserAvatarUrl = currentUserAvatarUrl,
                        posts = posts,
                        leaderboard = leaderboard,
                        milestones = milestones,
                        onBack = { finish() },
                        onToggleDarkMode = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onPostInputChange = { postInput = it },
                        onPickImage = { imagePicker.launch("image/*") },
                        onClearImage = {
                            selectedImageUriText = ""
                            statusMessage = "Photo removed."
                        },
                        onPublish = {
                            val message = postInput.trim()
                            if (message.isBlank()) {
                                statusMessage = "Write something before posting."
                                return@CommunityScreen
                            }

                            val stats = getStats()
                            val selectedImageUri = selectedImageUriText.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

                            if (selectedImageUri != null) {
                                statusMessage = "Uploading photo..."
                                uploadPostImage(selectedImageUri, stats) { uploadedUrl, error ->
                                    if (error != null || uploadedUrl.isNullOrBlank()) {
                                        statusMessage = "Post failed: ${error ?: "image upload error"}"
                                        return@uploadPostImage
                                    }

                                    statusMessage = "Publishing update..."
                                    publishPost(message, uploadedUrl, stats) {
                                        postInput = ""
                                        selectedImageUriText = ""
                                        statusMessage = "Update posted."
                                        refreshCommunity()
                                    }
                                }
                            } else {
                                statusMessage = "Publishing update..."
                                publishPost(message, "", stats) {
                                    postInput = ""
                                    statusMessage = "Update posted."
                                    refreshCommunity()
                                }
                            }
                        },
                        onLikePost = { postId ->
                            likePost(postId) {
                                refreshCommunity()
                            }
                        },
                        onRefresh = {
                            statusMessage = "Refreshing community..."
                            refreshCommunity()
                            statusMessage = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityScreen(
    darkMode: Boolean,
    statusMessage: String,
    postInput: String,
    selectedImageUriText: String,
    currentUserAvatarUrl: String,
    posts: List<CommunityPost>,
    leaderboard: List<LeaderboardEntry>,
    milestones: List<MilestoneItem>,
    onBack: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onPostInputChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onPublish: () -> Unit,
    onLikePost: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val shellColor = if (darkMode) Color(0xFF1E1E1E) else Color(0xFFF0F2F5)
    val barColor = if (darkMode) Color(0xFF2D4373) else Color(0xFF3B5998)
    val cardColor = if (darkMode) Color(0xFF292929) else Color.White
    val borderColor = if (darkMode) Color(0xFF3A3A3A) else Color(0xFFD8DEE8)
    val textColor = if (darkMode) Color(0xFFF2F4F8) else Color(0xFF1C1E21)
    val mutedText = if (darkMode) Color(0xFFBFC7D5) else Color(0xFF5A5F6A)

    Column(modifier = Modifier.fillMaxSize().background(shellColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("Back", color = Color.White) }
                Text(
                    text = "greenifybook",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            TextButton(onClick = { onToggleDarkMode(!darkMode) }) {
                Text(if (darkMode) "Light" else "Dark", color = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ComposerCard(
                    postInput = postInput,
                    selectedImageUriText = selectedImageUriText,
                    currentUserAvatarUrl = currentUserAvatarUrl,
                    statusMessage = statusMessage,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    mutedText = mutedText,
                    onPostInputChange = onPostInputChange,
                    onPickImage = onPickImage,
                    onClearImage = onClearImage,
                    onPublish = onPublish,
                    onRefresh = onRefresh
                )
            }

            item {
                LeaderboardStrip(
                    leaderboard = leaderboard,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    mutedText = mutedText
                )
            }

            item {
                MilestoneStrip(
                    milestones = milestones,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    mutedText = mutedText
                )
            }

            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    mutedText = mutedText,
                    onLike = { onLikePost(post.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ComposerCard(
    postInput: String,
    selectedImageUriText: String,
    currentUserAvatarUrl: String,
    statusMessage: String,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    mutedText: Color,
    onPostInputChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onPublish: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarBubble(imageUrl = currentUserAvatarUrl, size = 36.dp)
                Text("What's on your eco mind?", color = textColor, fontWeight = FontWeight.SemiBold)
            }

            OutlinedTextField(
                value = postInput,
                onValueChange = onPostInputChange,
                label = { Text("Share an update") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            if (selectedImageUriText.isNotBlank()) {
                AsyncImage(
                    model = Uri.parse(selectedImageUriText),
                    contentDescription = "Selected photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .background(Color(0xFFE7EBF2)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPickImage, modifier = Modifier.weight(1f)) { Text("Choose Photo") }
                Button(onClick = onClearImage, modifier = Modifier.weight(1f)) { Text("Remove") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPublish, modifier = Modifier.weight(1f)) { Text("Post") }
                Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text("Refresh") }
            }

            if (statusMessage.isNotBlank()) {
                Text(statusMessage, color = mutedText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LeaderboardStrip(
    leaderboard: List<LeaderboardEntry>,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    mutedText: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Weekly Leaderboard", fontWeight = FontWeight.Bold, color = textColor)
            leaderboard.take(5).forEachIndexed { idx, entry ->
                Text(
                    text = "${idx + 1}. ${entry.userName} - ${entry.points} pts - ${entry.streak}d - ${String.format(Locale.US, "%.1f", entry.co2SavedWeek)} kg",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MilestoneStrip(
    milestones: List<MilestoneItem>,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    mutedText: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Wins & Milestones", fontWeight = FontWeight.Bold, color = textColor)
            milestones.forEach { milestone ->
                Text(
                    text = "${milestone.title}: ${milestone.subtitle}",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PostCard(
    post: CommunityPost,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    mutedText: Color,
    onLike: () -> Unit
) {
    val cardShape = RoundedCornerShape(8.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarBubble(imageUrl = post.userAvatarUrl, size = 34.dp)
                    Column {
                        Text(post.userName, color = textColor, fontWeight = FontWeight.SemiBold)
                        Text(formatPostTime(post.createdAtMs), color = mutedText, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (post.isMilestone) {
                    Text("Milestone", color = Color(0xFF2E7D32), style = MaterialTheme.typography.labelSmall)
                }
            }

            Text(
                text = post.message,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (post.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .background(Color(0xFFE7EBF2)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${post.likes} likes",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${String.format(Locale.US, "%.1f", post.co2SavedWeek)} kg CO2 • ${post.points} pts",
                    color = mutedText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Divider(color = borderColor)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionTextButton(text = "Like", onClick = onLike, color = mutedText)
                ActionTextButton(text = "Comment", onClick = {}, color = mutedText)
                ActionTextButton(text = "Share", onClick = {}, color = mutedText)
            }
        }
    }
}

@Composable
private fun AvatarBubble(imageUrl: String, size: androidx.compose.ui.unit.Dp) {
    if (imageUrl.isNotBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFE7EBF2)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF93ABC9))
        )
    }
}

@Composable
private fun ActionTextButton(text: String, onClick: () -> Unit, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatPostTime(createdAtMs: Long): String {
    if (createdAtMs <= 0L) return "Just now"
    return SimpleDateFormat("MMM d 'at' h:mm a", Locale.US).format(Date(createdAtMs))
}