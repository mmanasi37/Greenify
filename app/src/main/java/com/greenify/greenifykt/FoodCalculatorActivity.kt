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
import androidx.compose.runtime.LaunchedEffect
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

class FoodCalculatorActivity : ComponentActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val tipsRepository by lazy { EcoTipsRepository(db = db) }
    private val emissionsRepository by lazy {
        FoodEmissionsRepository(db = db, functions = Firebase.functions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)

        setContent {
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FoodCalculatorScreen(
                        darkMode = darkMode,
                        onToggleDarkMode = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onBack = { finish() },
                        onHome = {
                            startActivity(Intent(this, MainActivity::class.java))
                        },
                        onNavigateToElectricity = {
                            startActivity(Intent(this, ElectricityCalculatorActivity::class.java))
                        },
                        onNavigateToFood = { },
                        onNavigateToTransport = {
                            startActivity(Intent(this, TransportCalculatorActivity::class.java))
                        },
                        loadFoodOptions = { onLoaded ->
                            fetchFoodNames(onLoaded)
                        },
                        onCalculate = { foodType, amount, onResult ->
                            emissionsRepository.estimateFood(foodType, amount, onResult)
                        },
                        onGetTip = { onResult ->
                            getEcoTip(onResult)
                        }
                    )
                }
            }
        }
    }

    private fun fetchFoodNames(onLoaded: (List<String>) -> Unit) {
        val fallback = listOf(
            "beef",
            "chicken",
            "pork",
            "lamb",
            "fish",
            "rice",
            "milk",
            "cheese",
            "eggs",
            "tofu"
        )

        db.collection("foods").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onLoaded(fallback)
                return@addOnCompleteListener
            }

            val querySnapshot = task.result as? QuerySnapshot
            val foodNames = querySnapshot
                ?.documents
                ?.mapNotNull { it.getString("name")?.trim()?.lowercase() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?.sorted()
                .orEmpty()

            if (foodNames.isNotEmpty()) onLoaded(foodNames) else onLoaded(fallback)
        }
    }

    private fun getEcoTip(onResult: (String) -> Unit) {
        tipsRepository.getRandomTip(TipCategory.FOOD, onResult)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodCalculatorScreen(
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNavigateToElectricity: () -> Unit,
    onNavigateToFood: () -> Unit,
    onNavigateToTransport: () -> Unit,
    loadFoodOptions: ((List<String>) -> Unit) -> Unit,
    onCalculate: (String, String, (String) -> Unit) -> Unit,
    onGetTip: ((String) -> Unit) -> Unit
) {
    var foodType by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var emissionsResultText by remember { mutableStateOf("Your carbon result will appear here.") }
    var tipText by remember { mutableStateOf("Your food-related tip will appear here.") }
    var foodOptions by remember { mutableStateOf(emptyList<String>()) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadFoodOptions { fetched ->
            foodOptions = fetched
        }
    }

    val filteredFood = foodOptions.filter {
        it.contains(foodType.trim(), ignoreCase = true)
    }

    CalculatorScaffold(
        title = "Food Calculator",
        darkMode = darkMode,
        onToggleDarkMode = onToggleDarkMode,
        activeTab = CalculatorTab.FOOD,
        onBack = onBack,
        onHome = onHome,
        onNavigateToElectricity = onNavigateToElectricity,
        onNavigateToFood = onNavigateToFood,
        onNavigateToTransport = onNavigateToTransport
    ) {
        ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = {
                menuExpanded = !menuExpanded && filteredFood.isNotEmpty()
            }
        ) {
            OutlinedTextField(
                value = foodType,
                onValueChange = {
                    foodType = it
                    menuExpanded = filteredFood.isNotEmpty()
                },
                label = { Text("Type of food") },
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
                filteredFood.take(10).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            foodType = option
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount eaten (kg)") },
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
                    onCalculate(foodType, amount) { text ->
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
                    text = "Food Tip",
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
