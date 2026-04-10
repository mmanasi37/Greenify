package com.greenify.greenifykt

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import java.util.Locale
import java.util.concurrent.TimeUnit

class FoodEmissionsRepository(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    private val localFoodFactors = mapOf(
        "beef" to 27.0,
        "chicken" to 6.9,
        "pork" to 12.1,
        "lamb" to 39.2,
        "fish" to 6.0,
        "rice" to 2.7,
        "milk" to 1.9,
        "cheese" to 13.5,
        "eggs" to 4.8,
        "tofu" to 2.0
    )

    fun estimateFood(foodType: String, amountText: String, onResult: (String) -> Unit) {
        val normalizedFood = foodType.trim().lowercase(Locale.getDefault())
        val amount = amountText.toDoubleOrNull()

        if (normalizedFood.isBlank() || amount == null) {
            onResult("Please provide food type and a valid amount.")
            return
        }

        val key = "${normalizedFood}_${"%.2f".format(Locale.US, amount)}"

        fetchFreshCache(key) { cachedValue ->
            if (cachedValue != null) {
                onResult("Estimated emissions: $cachedValue kg CO2e (cached)")
                return@fetchFreshCache
            }

            requestLiveEstimate(normalizedFood, amount) { liveValue, source ->
                if (liveValue != null) {
                    writeCache(key, liveValue, source)
                    onResult("Estimated emissions: $liveValue kg CO2e ($source)")
                } else {
                    fallbackFromExistingCollection(normalizedFood, amount, onResult)
                }
            }
        }
    }

    private fun fetchFreshCache(key: String, onDone: (Double?) -> Unit) {
        db.collection("emission_cache_food").document(key).get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onDone(null)
                return@addOnCompleteListener
            }

            val doc = task.result
            if (doc == null || !doc.exists()) {
                onDone(null)
                return@addOnCompleteListener
            }

            val value = doc.getDouble("kg_co2e")
            val cachedAt = doc.getLong("cached_at_ms")
            val maxAgeMs = TimeUnit.HOURS.toMillis(24)
            val isFresh = cachedAt != null && (System.currentTimeMillis() - cachedAt) <= maxAgeMs

            if (value != null && isFresh) onDone(value) else onDone(null)
        }
    }

    private fun requestLiveEstimate(foodType: String, amount: Double, onDone: (Double?, String) -> Unit) {
        val payload = hashMapOf(
            "food" to foodType,
            "amount" to amount,
            "amount_unit" to "kg"
        )

        functions.getHttpsCallable("estimateFoodEmissions")
            .call(payload)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onDone(null, "fallback")
                    return@addOnCompleteListener
                }

                val parsed = parseCallableResult(task.result)
                onDone(parsed, "live")
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseCallableResult(result: HttpsCallableResult?): Double? {
        val data = result?.data as? Map<String, Any?> ?: return null
        val direct = (data["kg_co2e"] as? Number)?.toDouble()
        if (direct != null) return direct
        val alt = (data["kgCO2e"] as? Number)?.toDouble()
        if (alt != null) return alt
        val nested = data["data"] as? Map<String, Any?>
        return (nested?.get("kg_co2e") as? Number)?.toDouble()
            ?: (nested?.get("kgCO2e") as? Number)?.toDouble()
    }

    private fun writeCache(key: String, value: Double, source: String) {
        val payload = hashMapOf(
            "kg_co2e" to value,
            "source" to source,
            "cached_at_ms" to System.currentTimeMillis()
        )
        db.collection("emission_cache_food").document(key).set(payload)
    }

    private fun fallbackFromExistingCollection(foodType: String, amount: Double, onResult: (String) -> Unit) {
        db.collection("foods")
            .whereEqualTo("name", foodType)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val localEstimate = calculateLocalEstimate(foodType, amount)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val snapshot = task.result as? QuerySnapshot
                val docs = snapshot?.documents
                if (docs.isNullOrEmpty()) {
                    val localEstimate = calculateLocalEstimate(foodType, amount)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val factor = docs[0].getDouble("carbon_footprint")
                if (factor == null) {
                    val localEstimate = calculateLocalEstimate(foodType, amount)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val result = amount * factor
                onResult("Estimated emissions: $result kg CO2e (fallback)")
            }
    }

    private fun calculateLocalEstimate(foodType: String, amount: Double): Double {
        val factor = localFoodFactors[foodType] ?: 6.0
        return amount * factor
    }
}
