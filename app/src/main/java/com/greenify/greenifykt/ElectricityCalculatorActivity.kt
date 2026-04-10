package com.greenify.greenifykt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class ElectricityCalculatorActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val tipsRepository by lazy { EcoTipsRepository(db = db) }
    private val emissionsRepository by lazy {
        ElectricityEmissionsRepository(db = db, functions = Firebase.functions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ElectricityCalculatorScreen(
                        darkMode = darkMode,
                        onToggleDarkMode = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onBack = { finish() },
                        onHome = {
                            startActivity(Intent(this, MainActivity::class.java))
                        },
                        onNavigateToElectricity = { },
                        onNavigateToFood = {
                            startActivity(Intent(this, FoodCalculatorActivity::class.java))
                        },
                        onNavigateToTransport = {
                            startActivity(Intent(this, TransportCalculatorActivity::class.java))
                        },
                        loadApplianceOptions = { onLoaded ->
                            fetchApplianceNames(onLoaded)
                        },
                        onCalculate = { appliance, timeSpent, onResult ->
                            emissionsRepository.estimateElectricity(appliance, timeSpent, onResult)
                        },
                        onGetTip = { onResult ->
                            getEcoTip(onResult)
                        }
                    )
                }
            }
        }
    }

    private fun fetchApplianceNames(onLoaded: (List<String>) -> Unit) {
        val fallback = listOf(
            "air conditioner",
            "electric heater",
            "refrigerator",
            "washing machine",
            "dishwasher",
            "electric oven",
            "microwave",
            "desktop computer",
            "laptop",
            "television",
            "water pump",
            "lighting"
        )

        db.collection("appliances").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onLoaded(fallback)
                return@addOnCompleteListener
            }

            val querySnapshot = task.result as? QuerySnapshot
            val applianceNames = querySnapshot
                ?.documents
                ?.mapNotNull { it.getString("name")?.trim()?.lowercase() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                .orEmpty()

            if (applianceNames.isNotEmpty()) {
                onLoaded(applianceNames)
            } else {
                onLoaded(fallback)
            }
        }
    }

    private fun getEcoTip(onResult: (String) -> Unit) {
        tipsRepository.getRandomTip(TipCategory.ELECTRICITY, onResult)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElectricityCalculatorScreen(
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNavigateToElectricity: () -> Unit,
    onNavigateToFood: () -> Unit,
    onNavigateToTransport: () -> Unit,
    loadApplianceOptions: ((List<String>) -> Unit) -> Unit,
    onCalculate: (String, String, (String) -> Unit) -> Unit,
    onGetTip: ((String) -> Unit) -> Unit
) {
    var appliance by rememberSaveable { mutableStateOf("") }
    var timeSpent by rememberSaveable { mutableStateOf("") }
    var emissionsResultText by remember { mutableStateOf("Your carbon result will appear here.") }
    var tipText by remember { mutableStateOf("Your energy-saving tip will appear here.") }
    var applianceOptions by remember { mutableStateOf(emptyList<String>()) }
    var menuExpanded by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        loadApplianceOptions { fetched ->
            applianceOptions = fetched
        }
    }

    val filteredAppliances = applianceOptions.filter {
        it.contains(appliance.trim(), ignoreCase = true)
    }

    CalculatorScaffold(
        title = "Electricity Calculator",
        darkMode = darkMode,
        onToggleDarkMode = onToggleDarkMode,
        activeTab = CalculatorTab.ELECTRICITY,
        onBack = onBack,
        onHome = onHome,
        onNavigateToElectricity = onNavigateToElectricity,
        onNavigateToFood = onNavigateToFood,
        onNavigateToTransport = onNavigateToTransport
    ) {
        ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = {
                menuExpanded = !menuExpanded && filteredAppliances.isNotEmpty()
            }
        ) {
            OutlinedTextField(
                value = appliance,
                onValueChange = {
                    appliance = it
                    menuExpanded = filteredAppliances.isNotEmpty()
                },
                label = { Text("Type of electrical appliance") },
                supportingText = { Text("Pick from list or type your own") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                filteredAppliances.take(10).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            appliance = option
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = timeSpent,
            onValueChange = { timeSpent = it },
            label = { Text("Time spent (hours)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    onCalculate(appliance, timeSpent) { text ->
                        emissionsResultText = text
                    }
                }
            ) {
                Text("Calculate")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    onGetTip { text ->
                        tipText = text
                    }
                }
            ) {
                Text("Get Tips")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Emitted CO2",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = emissionsResultText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Energy Tip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = tipText,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
