package com.greenify.greenifykt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CalculatorTab {
    ELECTRICITY,
    FOOD,
    TRANSPORT
}

@Composable
fun CalculatorScaffold(
    title: String,
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    activeTab: CalculatorTab,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNavigateToElectricity: () -> Unit,
    onNavigateToFood: () -> Unit,
    onNavigateToTransport: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                TextButton(onClick = onHome) {
                    Text("Home")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dark", style = MaterialTheme.typography.labelMedium)
                Switch(checked = darkMode, onCheckedChange = onToggleDarkMode)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Greenify",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorNavButton(
                label = "Electricity",
                active = activeTab == CalculatorTab.ELECTRICITY,
                onClick = onNavigateToElectricity,
                modifier = Modifier.weight(1f)
            )
            CalculatorNavButton(
                label = "Food",
                active = activeTab == CalculatorTab.FOOD,
                onClick = onNavigateToFood,
                modifier = Modifier.weight(1f)
            )
            CalculatorNavButton(
                label = "Transport",
                active = activeTab == CalculatorTab.TRANSPORT,
                onClick = onNavigateToTransport,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        content()
    }
}

@Composable
private fun CalculatorNavButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (active) {
        Button(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label)
        }
    }
}
