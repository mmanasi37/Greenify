package com.greenify.greenifykt

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.firestore.QuerySnapshot
import java.util.Locale
import java.util.concurrent.TimeUnit

class ElectricityEmissionsRepository(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    private val localApplianceFactorsWh = mapOf(
        "air conditioner" to 1200.0,
        "electric heater" to 1800.0,
        "refrigerator" to 180.0,
        "washing machine" to 500.0,
        "dishwasher" to 1200.0,
        "electric oven" to 2000.0,
        "microwave" to 1100.0,
        "desktop computer" to 200.0,
        "laptop" to 60.0,
        "television" to 100.0,
        "water pump" to 750.0,
        "lighting" to 60.0
    )

    fun estimateElectricity(
        appliance: String,
        hoursText: String,
        onResult: (String) -> Unit
    ) {
        val normalizedAppliance = appliance.trim().lowercase(Locale.getDefault())
        val hours = hoursText.toDoubleOrNull()

        if (normalizedAppliance.isBlank() || hours == null) {
            onResult("Please provide appliance and a valid number of hours.")
            return
        }

        val key = "${normalizedAppliance}_${"%.2f".format(Locale.US, hours)}"

        fetchFreshCache(key) { cachedValue ->
            if (cachedValue != null) {
                onResult("Estimated emissions: $cachedValue kg CO2e (cached)")
                return@fetchFreshCache
            }

            requestLiveEstimate(normalizedAppliance, hours) { liveValue, source ->
                if (liveValue != null) {
                    writeCache(key, liveValue, source)
                    onResult("Estimated emissions: $liveValue kg CO2e ($source)")
                } else {
                    fallbackFromExistingCollection(normalizedAppliance, hours, onResult)
                }
            }
        }
    }

    private fun fetchFreshCache(key: String, onDone: (Double?) -> Unit) {
        db.collection("emission_cache_electricity").document(key).get().addOnCompleteListener { task ->
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

            if (value != null && isFresh) {
                onDone(value)
            } else {
                onDone(null)
            }
        }
    }

    private fun requestLiveEstimate(appliance: String, hours: Double, onDone: (Double?, String) -> Unit) {
        val payload = hashMapOf(
            "appliance" to appliance,
            "hours" to hours
        )

        functions
            .getHttpsCallable("estimateElectricityEmissions")
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
        db.collection("emission_cache_electricity").document(key).set(payload)
    }

    private fun fallbackFromExistingCollection(appliance: String, hours: Double, onResult: (String) -> Unit) {
        db.collection("appliances")
            .whereEqualTo("name", appliance)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val localEstimate = calculateLocalEstimate(appliance, hours)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val snapshot = task.result as? QuerySnapshot
                val docs = snapshot?.documents
                if (docs.isNullOrEmpty()) {
                    val localEstimate = calculateLocalEstimate(appliance, hours)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val factor = docs[0].getDouble("carbon_footprint")
                if (factor == null) {
                    val localEstimate = calculateLocalEstimate(appliance, hours)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val result = hours * factor * 1.0e-3
                onResult("Estimated emissions: $result kg CO2e (fallback)")
            }
    }

    private fun calculateLocalEstimate(appliance: String, hours: Double): Double {
        val factorWh = localApplianceFactorsWh[appliance] ?: 500.0
        return (hours * factorWh) / 1_000_000.0
    }
}
