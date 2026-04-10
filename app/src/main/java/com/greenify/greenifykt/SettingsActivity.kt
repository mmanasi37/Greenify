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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : ComponentActivity() {
    private fun settingsPrefs() = getSharedPreferences("greenify_settings", MODE_PRIVATE)
    private fun challengePrefs() = getSharedPreferences("greenify_challenge", MODE_PRIVATE)
    private fun dashboardPrefs() = getSharedPreferences("greenify_dashboard", MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        setContent {
            val prefs = settingsPrefs()
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }
            var remindersEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean("tips_reminders_enabled", true)) }
            var autoRefreshStore by rememberSaveable { mutableStateOf(prefs.getBoolean("estore_auto_refresh", true)) }
            var impactInKg by rememberSaveable { mutableStateOf(prefs.getBoolean("impact_in_kg", true)) }
            var statusMessage by rememberSaveable { mutableStateOf("") }
            val user = FirebaseAuth.getInstance().currentUser
            val accountLabel = user?.email ?: "Signed in account"
            val accountUid = user?.uid ?: "Not signed in"

            fun setBooleanPref(key: String, value: Boolean) {
                prefs.edit().putBoolean(key, value).apply()
            }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        darkMode = darkMode,
                        remindersEnabled = remindersEnabled,
                        autoRefreshStore = autoRefreshStore,
                        impactInKg = impactInKg,
                        statusMessage = statusMessage,
                        accountLabel = accountLabel,
                        accountUid = accountUid,
                        onToggleDarkMode = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                            statusMessage = "Theme updated."
                        },
                        onToggleReminders = {
                            remindersEnabled = it
                            setBooleanPref("tips_reminders_enabled", it)
                            statusMessage = if (it) "Daily tips reminders enabled." else "Daily tips reminders disabled."
                        },
                        onToggleStoreAutoRefresh = {
                            autoRefreshStore = it
                            setBooleanPref("estore_auto_refresh", it)
                            statusMessage = if (it) "Live store refresh enabled." else "Live store refresh disabled."
                        },
                        onToggleImpactUnits = {
                            impactInKg = it
                            setBooleanPref("impact_in_kg", it)
                            statusMessage = if (it) "Impact unit set to kg CO2." else "Impact unit set to points."
                        },
                        onResetLocalProgress = {
                            challengePrefs().edit().clear().apply()
                            dashboardPrefs().edit().clear().apply()
                            statusMessage = "Challenge and dashboard progress reset."
                        },
                        onSignOut = {
                            FirebaseAuth.getInstance().signOut()
                            statusMessage = "Signed out."
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        },
                        onOpenProfile = {
                            startActivity(Intent(this, ProfileActivity::class.java))
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    darkMode: Boolean,
    remindersEnabled: Boolean,
    autoRefreshStore: Boolean,
    impactInKg: Boolean,
    statusMessage: String,
    accountLabel: String,
    accountUid: String,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onToggleStoreAutoRefresh: (Boolean) -> Unit,
    onToggleImpactUnits: (Boolean) -> Unit,
    onResetLocalProgress: () -> Unit,
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Account", fontWeight = FontWeight.SemiBold)
                Text(accountLabel, style = MaterialTheme.typography.bodySmall)
                Text("UID: $accountUid", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onOpenProfile) {
                    Text("Open Profile")
                }
                OutlinedButton(onClick = onSignOut) {
                    Text("Sign Out")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dark Mode", fontWeight = FontWeight.SemiBold)
                    Text("Apply appearance across the whole app", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = darkMode, onCheckedChange = onToggleDarkMode)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Tips Reminder", fontWeight = FontWeight.SemiBold)
                    Text("Toggle eco tips reminder preference", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = remindersEnabled, onCheckedChange = onToggleReminders)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Live eStore Refresh", fontWeight = FontWeight.SemiBold)
                    Text("Auto-sync store products from Firestore", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = autoRefreshStore, onCheckedChange = onToggleStoreAutoRefresh)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Impact Unit", fontWeight = FontWeight.SemiBold)
                    Text(if (impactInKg) "Show impact as kg CO2" else "Show impact as points", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = impactInKg, onCheckedChange = onToggleImpactUnits)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Data Management", fontWeight = FontWeight.SemiBold)
                Text("Reset local challenge and dashboard tracking data.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onResetLocalProgress, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset Local Progress")
                }
            }
        }

        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2E7D32)
            )
        }
    }
}
