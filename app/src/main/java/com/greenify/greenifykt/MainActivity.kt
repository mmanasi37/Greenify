package com.greenify.greenifykt

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getDashboardPrefs() = getSharedPreferences("greenify_dashboard", MODE_PRIVATE)
    private fun getProfilePrefs() = getSharedPreferences("greenify_profile", MODE_PRIVATE)
    private fun getFirstName(): String = getProfilePrefs().getString("first_name", "")?.trim().orEmpty()

    private fun todayKey(): String = dateFormatter.format(Date())

    private fun getCompletedDates(): MutableSet<String> {
        return getDashboardPrefs().getStringSet("tip_completed_dates", emptySet())?.toMutableSet()
            ?: mutableSetOf()
    }

    private fun setCompletedToday(completed: Boolean) {
        val dates = getCompletedDates()
        val today = todayKey()
        if (completed) dates.add(today) else dates.remove(today)
        getDashboardPrefs().edit().putStringSet("tip_completed_dates", dates).apply()
    }

    private fun isCompletedToday(): Boolean = getCompletedDates().contains(todayKey())

    private fun getWeeklyStreakCount(): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val weekStart = cal.time
        return getCompletedDates().count { dateText ->
            try {
                val date = dateFormatter.parse(dateText)
                date != null && !date.before(weekStart)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun isTipFavorite(text: String): Boolean {
        val saved = getDashboardPrefs().getStringSet("favorite_tips", emptySet()).orEmpty()
        return saved.contains(text)
    }

    private fun setTipFavorite(text: String, favorite: Boolean) {
        val saved = getDashboardPrefs().getStringSet("favorite_tips", emptySet())?.toMutableSet()
            ?: mutableSetOf()
        if (favorite) saved.add(text) else saved.remove(text)
        getDashboardPrefs().edit().putStringSet("favorite_tips", saved).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth?.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }
            var firstName by rememberSaveable { mutableStateOf(getFirstName()) }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        darkMode = ThemeModeManager.isDarkModeEnabled(this@MainActivity)
                        firstName = getFirstName()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val tipsRepository = remember { EcoTipsRepository(FirebaseFirestore.getInstance()) }
            var dashboardTip by remember {
                mutableStateOf(
                    EcoTip(
                        text = "Tap Show Another Tip to load an eco-friendly suggestion.",
                        category = TipCategory.GENERAL,
                        impactKgPerWeek = 0.6,
                        difficulty = "Easy",
                        startMinutes = 2,
                        source = "Greenify",
                        updatedLabel = "Updated today"
                    )
                )
            }
            var completedToday by rememberSaveable { mutableStateOf(isCompletedToday()) }
            var weeklyStreak by rememberSaveable { mutableStateOf(getWeeklyStreakCount()) }
            var isFavorite by rememberSaveable { mutableStateOf(false) }

            fun refreshTip() {
                tipsRepository.getRandomTipDetail { tip ->
                    dashboardTip = tip
                    isFavorite = isTipFavorite(tip.text)
                    completedToday = isCompletedToday()
                    weeklyStreak = getWeeklyStreakCount()
                    tipsRepository.logTipEvent(
                        eventType = "tip_viewed",
                        tip = tip,
                        screen = "dashboard",
                        userId = auth?.currentUser?.uid
                    )
                }
            }

            LaunchedEffect(Unit) {
                tipsRepository.seedTipsIfEmpty()
                refreshTip()
            }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        firstName = firstName,
                        dashboardTip = dashboardTip,
                        completedToday = completedToday,
                        weeklyStreak = weeklyStreak,
                        isFavorite = isFavorite,
                        onRefreshTip = { refreshTip() },
                        onToggleCompletedToday = { checked ->
                            completedToday = checked
                            setCompletedToday(checked)
                            weeklyStreak = getWeeklyStreakCount()
                            if (checked) {
                                tipsRepository.logTipEvent(
                                    eventType = "tip_completed",
                                    tip = dashboardTip,
                                    screen = "dashboard",
                                    userId = auth?.currentUser?.uid
                                )
                            }
                        },
                        onToggleFavorite = { favorite ->
                            isFavorite = favorite
                            setTipFavorite(dashboardTip.text, favorite)
                            if (favorite) {
                                tipsRepository.logTipEvent(
                                    eventType = "tip_saved",
                                    tip = dashboardTip,
                                    screen = "dashboard",
                                    userId = auth?.currentUser?.uid
                                )
                            }
                        },
                        onShareTip = {
                            val shareText = buildString {
                                append("Eco Tip: ${dashboardTip.text}\n")
                                append("Category: ${dashboardTip.category.name.lowercase().replaceFirstChar { c -> c.uppercase() }}\n")
                                append("Estimated impact: ~${String.format(Locale.US, "%.1f", dashboardTip.impactKgPerWeek)} kg CO2/week")
                            }
                            tipsRepository.logTipEvent(
                                eventType = "tip_shared",
                                tip = dashboardTip,
                                screen = "dashboard",
                                userId = auth?.currentUser?.uid
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            startActivity(Intent.createChooser(intent, "Share eco tip"))
                        },
                        onOpenCalculator = { startActivity(Intent(this, ElectricityCalculatorActivity::class.java)) },
                        onOpenTips = { startActivity(Intent(this, EcoTips::class.java)) },
                        onOpenChallenge = { startActivity(Intent(this, Challenge::class.java)) },
                        onOpenCommunity = { startActivity(Intent(this, Community::class.java)) },
                        onOpenStore = { startActivity(Intent(this, eStore::class.java)) },
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                        onLogout = {
                            auth?.signOut()
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    firstName: String,
    dashboardTip: EcoTip,
    completedToday: Boolean,
    weeklyStreak: Int,
    isFavorite: Boolean,
    onRefreshTip: () -> Unit,
    onToggleCompletedToday: (Boolean) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onShareTip: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenTips: () -> Unit,
    onOpenChallenge: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val headerGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(headerGradient)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (firstName.isNotBlank()) {
                            Text(
                                text = "Hi $firstName!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "Welcome to Greenify",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Track, reduce, and improve your sustainability journey.",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DashboardRow(
            left = DashboardItem("Carbon Calculator", "Measure your footprint", "CO2", onOpenCalculator),
            right = DashboardItem("Eco Tips", "Practical daily actions", "TIP", onOpenTips)
        )

        Spacer(modifier = Modifier.height(10.dp))

        DashboardRow(
            left = DashboardItem("Challenge", "Complete green missions", "GO", onOpenChallenge),
            right = DashboardItem("Community", "Share progress", "COM", onOpenCommunity)
        )

        Spacer(modifier = Modifier.height(10.dp))

        DashboardRow(
            left = DashboardItem("eStore", "Eco-friendly picks", "BUY", onOpenStore),
            right = DashboardItem("Settings", "App preferences", "SET", onOpenSettings)
        )

        Spacer(modifier = Modifier.height(12.dp))

        EcoTipInsightPanel(
            title = "Daily Eco Tip",
            tip = dashboardTip,
            completedToday = completedToday,
            weeklyStreak = weeklyStreak,
            isFavorite = isFavorite,
            onRefreshTip = onRefreshTip,
            onToggleCompletedToday = onToggleCompletedToday,
            onToggleFavorite = onToggleFavorite,
            onShareTip = onShareTip,
            primaryActionLabel = "Show Another Tip",
            secondaryActionLabel = "Open Tips",
            onSecondaryAction = onOpenTips
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

private data class DashboardItem(
    val title: String,
    val subtitle: String,
    val icon: String,
    val onClick: () -> Unit
)

@Composable
private fun DashboardRow(left: DashboardItem, right: DashboardItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DashboardCard(item = left, modifier = Modifier.weight(1f))
        DashboardCard(item = right, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DashboardCard(item: DashboardItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { item.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = item.icon, fontSize = 28.sp, modifier = Modifier.size(32.dp))
            Column {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
