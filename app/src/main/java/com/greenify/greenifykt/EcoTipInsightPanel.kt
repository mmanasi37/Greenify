package com.greenify.greenifykt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun EcoTipInsightPanel(
    title: String,
    tip: EcoTip,
    completedToday: Boolean,
    weeklyStreak: Int,
    isFavorite: Boolean,
    onRefreshTip: () -> Unit,
    onToggleCompletedToday: (Boolean) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onShareTip: () -> Unit,
    primaryActionLabel: String = "Show Another Tip",
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = tip.text,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Category: ${formatCategory(tip.category)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Best for: ${tipContextLabel(tip.category)}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Estimated impact: ~${String.format(Locale.US, "%.1f", tip.impactKgPerWeek)} kg CO2/week",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Difficulty: ${tip.difficulty} | Start in: ${tip.startMinutes} min",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = completedToday, onCheckedChange = onToggleCompletedToday)
                Text(
                    text = "Try this today",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Weekly streak: $weeklyStreak eco actions",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onToggleFavorite(!isFavorite) }, modifier = Modifier.weight(1f)) {
                    Text(if (isFavorite) "Saved" else "Save Tip")
                }
                Button(onClick = onShareTip, modifier = Modifier.weight(1f)) {
                    Text("Share Tip")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefreshTip, modifier = Modifier.weight(1f)) {
                    Text(primaryActionLabel)
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Button(onClick = onSecondaryAction, modifier = Modifier.weight(1f)) {
                        Text(secondaryActionLabel)
                    }
                }
            }

            Text(
                text = "Source: ${tip.source} | ${tip.updatedLabel}",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatCategory(category: TipCategory): String {
    return category.name.lowercase().replaceFirstChar { it.uppercase() }
}

private fun tipContextLabel(category: TipCategory): String {
    return when (category) {
        TipCategory.GENERAL -> "Everyday lifestyle choices"
        TipCategory.ELECTRICITY -> "Home electricity and energy use"
        TipCategory.FOOD -> "Meals, food choices, and waste reduction"
        TipCategory.TRANSPORT -> "Travel and commuting habits"
    }
}
