package com.greenify.greenifykt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EcoTips : ComponentActivity() {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getDashboardPrefs() = getSharedPreferences("greenify_dashboard", MODE_PRIVATE)

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

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }
            val tipsRepository = remember { EcoTipsRepository(FirebaseFirestore.getInstance()) }
            var tip by remember {
                mutableStateOf(
                    EcoTip(
                        text = "Loading tip...",
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
                tipsRepository.getRandomTipDetail { loaded ->
                    tip = loaded
                    isFavorite = isTipFavorite(loaded.text)
                    completedToday = isCompletedToday()
                    weeklyStreak = getWeeklyStreakCount()
                    tipsRepository.logTipEvent(
                        eventType = "tip_viewed",
                        tip = loaded,
                        screen = "eco_tips",
                        userId = FirebaseAuth.getInstance().currentUser?.uid
                    )
                }
            }

            LaunchedEffect(Unit) {
                tipsRepository.seedTipsIfEmpty()
                refreshTip()
            }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EcoTipsScreen(
                        tip = tip,
                        completedToday = completedToday,
                        weeklyStreak = weeklyStreak,
                        isFavorite = isFavorite,
                        darkMode = darkMode,
                        onBack = { finish() },
                        onToggleDarkMode = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onNewTip = { refreshTip() },
                        onToggleCompletedToday = { checked ->
                            completedToday = checked
                            setCompletedToday(checked)
                            weeklyStreak = getWeeklyStreakCount()
                            if (checked) {
                                tipsRepository.logTipEvent(
                                    eventType = "tip_completed",
                                    tip = tip,
                                    screen = "eco_tips",
                                    userId = FirebaseAuth.getInstance().currentUser?.uid
                                )
                            }
                        },
                        onToggleFavorite = { favorite ->
                            isFavorite = favorite
                            setTipFavorite(tip.text, favorite)
                            if (favorite) {
                                tipsRepository.logTipEvent(
                                    eventType = "tip_saved",
                                    tip = tip,
                                    screen = "eco_tips",
                                    userId = FirebaseAuth.getInstance().currentUser?.uid
                                )
                            }
                        },
                        onShareTip = {
                            val shareText = buildString {
                                append("Eco Tip: ${tip.text}\n")
                                append("Category: ${tip.category.name.lowercase().replaceFirstChar { c -> c.uppercase() }}\n")
                                append("Estimated impact: ~${String.format(Locale.US, "%.1f", tip.impactKgPerWeek)} kg CO2/week")
                            }
                            tipsRepository.logTipEvent(
                                eventType = "tip_shared",
                                tip = tip,
                                screen = "eco_tips",
                                userId = FirebaseAuth.getInstance().currentUser?.uid
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            startActivity(Intent.createChooser(intent, "Share eco tip"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EcoTipsScreen(
    tip: EcoTip,
    completedToday: Boolean,
    weeklyStreak: Int,
    isFavorite: Boolean,
    darkMode: Boolean,
    onBack: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onNewTip: () -> Unit,
    onToggleCompletedToday: (Boolean) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onShareTip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
            TextButton(onClick = { onToggleDarkMode(!darkMode) }) {
                Text(if (darkMode) "Light" else "Dark")
            }
        }

        Text(
            text = "Eco-Friendly Tips",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        EcoTipInsightPanel(
            title = "Tip of the Moment",
            tip = tip,
            completedToday = completedToday,
            weeklyStreak = weeklyStreak,
            isFavorite = isFavorite,
            onRefreshTip = onNewTip,
            onToggleCompletedToday = onToggleCompletedToday,
            onToggleFavorite = onToggleFavorite,
            onShareTip = onShareTip,
            primaryActionLabel = "Show Another Tip"
        )
    }
}