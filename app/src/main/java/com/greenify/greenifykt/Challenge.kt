package com.greenify.greenifykt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Challenge : ComponentActivity() {
    private val dailyChallenges = listOf(
        EcoChallenge("daily_lights", "Switch off unused lights for a day", 0.6, 20),
        EcoChallenge("daily_bottle", "Carry a reusable bottle today", 0.4, 20),
        EcoChallenge("daily_walk", "Walk for one short trip", 0.8, 20),
        EcoChallenge("daily_unplug", "Unplug idle chargers and devices", 0.5, 20)
    )

    private val weeklyChallenges = listOf(
        EcoChallenge("weekly_energy", "Reduce home electricity habits", 3.5, 75),
        EcoChallenge("weekly_food", "Plan low-emission meals this week", 4.2, 75),
        EcoChallenge("weekly_transport", "Choose greener transport for commute", 5.0, 75)
    )

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun prefs() = getSharedPreferences("greenify_challenge", MODE_PRIVATE)
    private fun profilePrefs() = getSharedPreferences("greenify_profile", MODE_PRIVATE)
    private fun getFirstName(): String = profilePrefs().getString("first_name", "")?.trim().orEmpty()

    private fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun todayKey(): String = dateFormatter.format(Date())

    private fun selectDailyChallenge(): EcoChallenge {
        val cal = Calendar.getInstance()
        val index = cal.get(Calendar.DAY_OF_YEAR) % dailyChallenges.size
        return dailyChallenges[index]
    }

    private fun selectWeeklyChallenge(): EcoChallenge {
        val cal = Calendar.getInstance()
        val index = cal.get(Calendar.WEEK_OF_YEAR) % weeklyChallenges.size
        return weeklyChallenges[index]
    }

    private fun isDailyCompleted(dailyId: String): Boolean {
        val saved = prefs().getStringSet("daily_completed", emptySet()).orEmpty()
        return saved.contains("${todayKey()}:$dailyId")
    }

    private fun setDailyCompleted(dailyId: String) {
        val saved = prefs().getStringSet("daily_completed", emptySet())?.toMutableSet() ?: mutableSetOf()
        saved.add("${todayKey()}:$dailyId")
        prefs().edit().putStringSet("daily_completed", saved).apply()
    }

    private fun clearDailyCompleted(dailyId: String) {
        val saved = prefs().getStringSet("daily_completed", emptySet())?.toMutableSet() ?: mutableSetOf()
        saved.remove("${todayKey()}:$dailyId")
        prefs().edit().putStringSet("daily_completed", saved).apply()
    }

    private fun getWeeklyProgress(weeklyId: String): Int {
        return prefs().getInt("${currentWeekKey()}:${weeklyId}:progress", 0)
    }

    private fun setWeeklyProgress(weeklyId: String, value: Int) {
        prefs().edit().putInt("${currentWeekKey()}:${weeklyId}:progress", value).apply()
    }

    private fun getPoints(): Int = prefs().getInt("points", 0)

    private fun updatePoints(delta: Int) {
        val updated = (getPoints() + delta).coerceAtLeast(0)
        prefs().edit().putInt("points", updated).apply()
    }

    private fun getWeeklyCo2Saved(): Double {
        return java.lang.Double.longBitsToDouble(
            prefs().getLong("${currentWeekKey()}:co2_saved_bits", java.lang.Double.doubleToRawLongBits(0.0))
        )
    }

    private fun addWeeklyCo2Saved(amount: Double) {
        val total = (getWeeklyCo2Saved() + amount).coerceAtLeast(0.0)
        prefs().edit().putLong("${currentWeekKey()}:co2_saved_bits", java.lang.Double.doubleToRawLongBits(total)).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }
            val firstName = remember { getFirstName() }
            var points by remember { mutableStateOf(getPoints()) }
            val dailyChallenge = remember { selectDailyChallenge() }
            val weeklyChallenge = remember { selectWeeklyChallenge() }
            var dailyDone by remember { mutableStateOf(isDailyCompleted(dailyChallenge.id)) }
            var weeklyProgress by remember { mutableStateOf(getWeeklyProgress(weeklyChallenge.id)) }
            var weeklyCo2Saved by remember { mutableStateOf(getWeeklyCo2Saved()) }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChallengeScreen(
                        firstName = firstName,
                        darkMode = darkMode,
                        points = points,
                        weeklyCo2Saved = weeklyCo2Saved,
                        dailyChallenge = dailyChallenge,
                        weeklyChallenge = weeklyChallenge,
                        dailyDone = dailyDone,
                        weeklyProgress = weeklyProgress,
                        weeklyTarget = 5,
                        onBack = { finish() },
                        onToggleDarkMode = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onCompleteDaily = {
                            if (!dailyDone) {
                                dailyDone = true
                                setDailyCompleted(dailyChallenge.id)
                                updatePoints(dailyChallenge.points)
                                addWeeklyCo2Saved(dailyChallenge.co2KgImpact)
                                points = getPoints()
                                weeklyCo2Saved = getWeeklyCo2Saved()
                            }
                        },
                        onUndoDaily = {
                            if (dailyDone) {
                                dailyDone = false
                                clearDailyCompleted(dailyChallenge.id)
                                updatePoints(-dailyChallenge.points)
                                addWeeklyCo2Saved(-dailyChallenge.co2KgImpact)
                                points = getPoints()
                                weeklyCo2Saved = getWeeklyCo2Saved()
                            }
                        },
                        onAddWeeklyAction = {
                            if (weeklyProgress < 5) {
                                val next = weeklyProgress + 1
                                weeklyProgress = next
                                setWeeklyProgress(weeklyChallenge.id, next)
                                updatePoints(15)
                                addWeeklyCo2Saved(weeklyChallenge.co2KgImpact / 5.0)
                                if (next == 5) {
                                    updatePoints(weeklyChallenge.points)
                                }
                                points = getPoints()
                                weeklyCo2Saved = getWeeklyCo2Saved()
                            }
                        },
                        onUndoWeeklyAction = {
                            if (weeklyProgress > 0) {
                                if (weeklyProgress == 5) {
                                    updatePoints(-weeklyChallenge.points)
                                }

                                val next = weeklyProgress - 1
                                weeklyProgress = next
                                setWeeklyProgress(weeklyChallenge.id, next)

                                updatePoints(-15)
                                addWeeklyCo2Saved(-(weeklyChallenge.co2KgImpact / 5.0))

                                points = getPoints()
                                weeklyCo2Saved = getWeeklyCo2Saved()
                            }
                        }
                    )
                }
            }
        }
    }
}

private data class EcoChallenge(
    val id: String,
    val title: String,
    val co2KgImpact: Double,
    val points: Int
)

@Composable
private fun ChallengeScreen(
    firstName: String,
    darkMode: Boolean,
    points: Int,
    weeklyCo2Saved: Double,
    dailyChallenge: EcoChallenge,
    weeklyChallenge: EcoChallenge,
    dailyDone: Boolean,
    weeklyProgress: Int,
    weeklyTarget: Int,
    onBack: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onCompleteDaily: () -> Unit,
    onUndoDaily: () -> Unit,
    onAddWeeklyAction: () -> Unit,
    onUndoWeeklyAction: () -> Unit
) {
    val level = (points / 100) + 1
    val levelProgress = (points % 100) / 100f
    val weeklyProgressRatio = weeklyProgress.toFloat() / weeklyTarget.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = { onToggleDarkMode(!darkMode) }) {
                Text(if (darkMode) "Light" else "Dark")
            }
        }

        if (firstName.isNotBlank()) {
            Text(
                text = "Hi $firstName!",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "Green Challenges",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Points: $points", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Level $level", color = MaterialTheme.colorScheme.onPrimaryContainer)
                LinearProgressIndicator(progress = levelProgress, modifier = Modifier.fillMaxWidth())
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily Challenge", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dailyChallenge.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Impact: ~${String.format(Locale.US, "%.1f", dailyChallenge.co2KgImpact)} kg CO2", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onCompleteDaily, enabled = !dailyDone, modifier = Modifier.fillMaxWidth()) {
                    Text(if (dailyDone) "Completed Today" else "Mark Daily Complete (+${dailyChallenge.points} pts)")
                }
                Button(onClick = onUndoDaily, enabled = dailyDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Undo Daily Completion")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weekly Challenge", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(weeklyChallenge.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Progress: $weeklyProgress / $weeklyTarget actions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = weeklyProgressRatio, modifier = Modifier.fillMaxWidth())
                Text("Milestone: ${milestoneLabel(weeklyProgressRatio)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onAddWeeklyAction, enabled = weeklyProgress < weeklyTarget, modifier = Modifier.fillMaxWidth()) {
                    Text(if (weeklyProgress >= weeklyTarget) "Weekly Complete" else "Log Weekly Action (+15 pts)")
                }
                Button(onClick = onUndoWeeklyAction, enabled = weeklyProgress > 0, modifier = Modifier.fillMaxWidth()) {
                    Text("Undo Last Weekly Action")
                }
                if (weeklyProgress >= weeklyTarget) {
                    Text("Weekly bonus earned: +${weeklyChallenge.points} pts", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your Impact This Week", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    text = "Estimated CO2 saved: ~${String.format(Locale.US, "%.1f", weeklyCo2Saved)} kg",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun milestoneLabel(progress: Float): String {
    return when {
        progress >= 1f -> "100% - Challenge Master"
        progress >= 0.75f -> "75% - Strong Momentum"
        progress >= 0.50f -> "50% - Halfway There"
        progress >= 0.25f -> "25% - Great Start"
        else -> "Start your first action"
    }
}