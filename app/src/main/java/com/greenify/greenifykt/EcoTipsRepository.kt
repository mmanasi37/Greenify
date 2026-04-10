package com.greenify.greenifykt

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

enum class TipCategory {
    GENERAL,
    ELECTRICITY,
    FOOD,
    TRANSPORT
}

data class EcoTip(
    val text: String,
    val category: TipCategory,
    val impactKgPerWeek: Double,
    val difficulty: String,
    val startMinutes: Int,
    val source: String,
    val updatedLabel: String
)

class EcoTipsRepository(
    private val db: FirebaseFirestore
) {
    private val fallbackTips = listOf(
        "Switch off lights and appliances when not in use.",
        "Use public transport, bike, or walk for short trips.",
        "Carry a reusable water bottle and avoid single-use plastics.",
        "Choose local and seasonal foods when possible.",
        "Wash clothes in cold water to reduce energy use.",
        "Unplug chargers and electronics when fully charged.",
        "Plan meals to reduce food waste at home.",
        "Use reusable bags, cups, and containers every day."
    )

    private val electricityFallbackTips = listOf(
        "Use LED bulbs to reduce electricity consumption.",
        "Turn off standby mode on TVs and chargers.",
        "Set air conditioner temperature to 24-26C to save energy.",
        "Run full-load washing cycles instead of multiple small loads."
    )

    private val foodFallbackTips = listOf(
        "Reduce red meat consumption and add more plant-based meals.",
        "Plan weekly meals to avoid food waste.",
        "Store leftovers safely and reuse them in the next meal.",
        "Buy local seasonal produce to lower food transport emissions."
    )

    private val transportFallbackTips = listOf(
        "Combine errands into one trip to reduce fuel use.",
        "Use public transport for regular commuting.",
        "Walk or cycle for short distances when possible.",
        "Keep tires properly inflated to improve fuel efficiency."
    )

    private val seedTips = listOf(
        mapOf(
            "tip" to "Switch to LED lighting and turn off unused lights.",
            "category" to "electricity",
            "impact_kg_week" to 0.9,
            "difficulty" to "Easy",
            "start_minutes" to 3,
            "source" to "Greenify",
            "updated_label" to "Updated this week"
        ),
        mapOf(
            "tip" to "Set AC between 24-26C to cut energy use.",
            "category" to "electricity",
            "impact_kg_week" to 1.2,
            "difficulty" to "Easy",
            "start_minutes" to 2,
            "source" to "Greenify",
            "updated_label" to "Updated this week"
        ),
        mapOf(
            "tip" to "Try one plant-based meal per day to reduce food emissions.",
            "category" to "food",
            "impact_kg_week" to 1.5,
            "difficulty" to "Medium",
            "start_minutes" to 5,
            "source" to "Greenify",
            "updated_label" to "Updated this week"
        ),
        mapOf(
            "tip" to "Plan meals in advance to prevent food waste.",
            "category" to "food",
            "impact_kg_week" to 1.1,
            "difficulty" to "Easy",
            "start_minutes" to 10,
            "source" to "Greenify",
            "updated_label" to "Updated this week"
        ),
        mapOf(
            "tip" to "Walk, cycle, or use transit for short daily trips.",
            "category" to "transport",
            "impact_kg_week" to 1.6,
            "difficulty" to "Medium",
            "start_minutes" to 4,
            "source" to "Greenify",
            "updated_label" to "Updated this week"
        ),
        mapOf(
            "tip" to "Combine errands into one route to reduce fuel burn.",
            "category" to "transport",
            "impact_kg_week" to 1.0,
            "difficulty" to "Easy",
            "start_minutes" to 6,
            "source" to "Greenify",
            "updated_label" to "Updated this week"
        ),
        mapOf(
            "tip" to "Carry reusables (bottle, bag, cup) whenever you go out.",
            "category" to "general",
            "impact_kg_week" to 0.6,
            "difficulty" to "Easy",
            "start_minutes" to 2,
            "source" to "Greenify",
            "updated_label" to "Updated this week"
        )
    )

    fun seedTipsIfEmpty() {
        db.collection("tips").limit(1).get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            val hasAnyTip = task.result?.documents?.isNotEmpty() == true
            if (hasAnyTip) {
                return@addOnCompleteListener
            }

            val batch = db.batch()
            seedTips.forEachIndexed { index, tipData ->
                val doc = db.collection("tips").document("seed_tip_${index + 1}")
                batch.set(doc, tipData)
            }
            batch.commit()
        }
    }

    fun logTipEvent(eventType: String, tip: EcoTip, screen: String, userId: String?) {
        val payload = hashMapOf(
            "event_type" to eventType,
            "tip_text" to tip.text,
            "tip_category" to tip.category.name.lowercase(),
            "screen" to screen,
            "impact_kg_week" to tip.impactKgPerWeek,
            "difficulty" to tip.difficulty,
            "start_minutes" to tip.startMinutes,
            "source" to tip.source,
            "updated_label" to tip.updatedLabel,
            "user_id" to (userId ?: "anonymous"),
            "created_at_ms" to System.currentTimeMillis()
        )

        db.collection("tip_events").add(payload)
    }

    fun getRandomTip(onResult: (String) -> Unit) {
        getRandomTip(TipCategory.GENERAL, onResult)
    }

    fun getRandomTipDetail(onResult: (EcoTip) -> Unit) {
        db.collection("tips").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onResult(createFallbackTip(TipCategory.GENERAL))
                return@addOnCompleteListener
            }

            val querySnapshot = task.result as? QuerySnapshot
            val documents = querySnapshot?.documents.orEmpty()

            val tips = documents.mapNotNull { doc ->
                val tip = doc.getString("tip")?.trim().orEmpty()
                if (tip.isBlank()) return@mapNotNull null

                val categoryValue = doc.getString("category")
                    ?: doc.getString("type")
                    ?: doc.getString("tip_type")
                    ?: doc.getString("scope")

                val category = parseCategory(categoryValue)
                EcoTip(
                    text = tip,
                    category = category,
                    impactKgPerWeek = (doc.getDouble("impact_kg_week") ?: defaultImpact(category)).coerceAtLeast(0.1),
                    difficulty = doc.getString("difficulty")?.trim().orEmpty().ifBlank { defaultDifficulty(category) },
                    startMinutes = (doc.getLong("start_minutes")?.toInt() ?: defaultStartMinutes(category)).coerceAtLeast(1),
                    source = doc.getString("source")?.trim().orEmpty().ifBlank { "Greenify" },
                    updatedLabel = doc.getString("updated_label")?.trim().orEmpty().ifBlank { "Updated today" }
                )
            }

            if (tips.isNotEmpty()) {
                onResult(tips.random())
            } else {
                onResult(createFallbackTip(TipCategory.GENERAL))
            }
        }
    }

    fun getRandomTip(category: TipCategory, onResult: (String) -> Unit) {
        db.collection("tips").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onResult(getFallbackByCategory(category).random())
                return@addOnCompleteListener
            }

            val querySnapshot = task.result as? QuerySnapshot
            val documents = querySnapshot?.documents.orEmpty()

            val scopedTips = documents
                .mapNotNull { doc ->
                    val tip = doc.getString("tip")?.trim().orEmpty()
                    if (tip.isBlank()) return@mapNotNull null

                    val categoryValue = doc.getString("category")
                        ?: doc.getString("type")
                        ?: doc.getString("tip_type")
                        ?: doc.getString("scope")

                    if (category == TipCategory.GENERAL || isCategoryMatch(categoryValue, category)) tip else null
                }
                .filter { it.isNotBlank() }
                .orEmpty()

            onResult(if (scopedTips.isNotEmpty()) scopedTips.random() else getFallbackByCategory(category).random())
        }
    }

    fun getTips(onResult: (List<String>) -> Unit) {
        db.collection("tips").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onResult(fallbackTips)
                return@addOnCompleteListener
            }

            val querySnapshot = task.result as? QuerySnapshot
            val tips = querySnapshot
                ?.documents
                ?.mapNotNull { it.getString("tip")?.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            onResult(if (tips.isNotEmpty()) tips else fallbackTips)
        }
    }

    private fun isCategoryMatch(value: String?, category: TipCategory): Boolean {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return when (category) {
            TipCategory.GENERAL -> true
            TipCategory.ELECTRICITY -> normalized in listOf("electricity", "energy", "power")
            TipCategory.FOOD -> normalized in listOf("food", "diet", "meal", "nutrition")
            TipCategory.TRANSPORT -> normalized in listOf("transport", "travel", "mobility", "commute")
        }
    }

    private fun getFallbackByCategory(category: TipCategory): List<String> {
        return when (category) {
            TipCategory.GENERAL -> fallbackTips
            TipCategory.ELECTRICITY -> electricityFallbackTips
            TipCategory.FOOD -> foodFallbackTips
            TipCategory.TRANSPORT -> transportFallbackTips
        }
    }

    private fun parseCategory(value: String?): TipCategory {
        val normalized = value?.trim()?.lowercase().orEmpty()
        return when {
            normalized in listOf("electricity", "energy", "power") -> TipCategory.ELECTRICITY
            normalized in listOf("food", "diet", "meal", "nutrition") -> TipCategory.FOOD
            normalized in listOf("transport", "travel", "mobility", "commute") -> TipCategory.TRANSPORT
            else -> TipCategory.GENERAL
        }
    }

    private fun defaultImpact(category: TipCategory): Double {
        return when (category) {
            TipCategory.GENERAL -> 0.6
            TipCategory.ELECTRICITY -> 0.9
            TipCategory.FOOD -> 1.2
            TipCategory.TRANSPORT -> 1.5
        }
    }

    private fun defaultDifficulty(category: TipCategory): String {
        return when (category) {
            TipCategory.GENERAL -> "Easy"
            TipCategory.ELECTRICITY -> "Easy"
            TipCategory.FOOD -> "Medium"
            TipCategory.TRANSPORT -> "Medium"
        }
    }

    private fun defaultStartMinutes(category: TipCategory): Int {
        return when (category) {
            TipCategory.GENERAL -> 2
            TipCategory.ELECTRICITY -> 3
            TipCategory.FOOD -> 5
            TipCategory.TRANSPORT -> 4
        }
    }

    private fun createFallbackTip(category: TipCategory): EcoTip {
        return EcoTip(
            text = getFallbackByCategory(category).random(),
            category = category,
            impactKgPerWeek = defaultImpact(category),
            difficulty = defaultDifficulty(category),
            startMinutes = defaultStartMinutes(category),
            source = "Greenify",
            updatedLabel = "Updated today"
        )
    }
}
