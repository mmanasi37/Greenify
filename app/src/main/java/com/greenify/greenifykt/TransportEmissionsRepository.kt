package com.greenify.greenifykt

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import java.util.Locale
import java.util.concurrent.TimeUnit

class TransportEmissionsRepository(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    private val localTransportFactors = mapOf(
        "car" to 0.192,
        "bus" to 0.105,
        "train" to 0.041,
        "motorbike" to 0.103,
        "bicycle" to 0.0,
        "walking" to 0.0,
        "taxi" to 0.192,
        "truck" to 0.27,
        "ferry" to 0.115,
        "airplane" to 0.255
    )

    fun estimateTransport(type: String, distanceText: String, onResult: (String) -> Unit) {
        val normalizedType = type.trim().lowercase(Locale.getDefault())
        val distance = distanceText.toDoubleOrNull()

        if (normalizedType.isBlank() || distance == null) {
            onResult("Please provide transport type and a valid distance.")
            return
        }

        val key = "${normalizedType}_${"%.2f".format(Locale.US, distance)}"

        fetchFreshCache(key) { cachedValue ->
            if (cachedValue != null) {
                onResult("Estimated emissions: $cachedValue kg CO2e (cached)")
                return@fetchFreshCache
            }

            requestLiveEstimate(normalizedType, distance) { liveValue, source ->
                if (liveValue != null) {
                    writeCache(key, liveValue, source)
                    onResult("Estimated emissions: $liveValue kg CO2e ($source)")
                } else {
                    fallbackFromExistingCollection(normalizedType, distance, onResult)
                }
            }
        }
    }

    private fun fetchFreshCache(key: String, onDone: (Double?) -> Unit) {
        db.collection("emission_cache_transport").document(key).get().addOnCompleteListener { task ->
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

    private fun requestLiveEstimate(type: String, distance: Double, onDone: (Double?, String) -> Unit) {
        val payload = hashMapOf(
            "transport" to type,
            "distance" to distance,
            "distance_unit" to "km"
        )

        functions.getHttpsCallable("estimateTransportEmissions")
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
        db.collection("emission_cache_transport").document(key).set(payload)
    }

    private fun fallbackFromExistingCollection(type: String, distance: Double, onResult: (String) -> Unit) {
        db.collection("transport")
            .whereEqualTo("name", type)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val localEstimate = calculateLocalEstimate(type, distance)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val snapshot = task.result as? QuerySnapshot
                val docs = snapshot?.documents
                if (docs.isNullOrEmpty()) {
                    val localEstimate = calculateLocalEstimate(type, distance)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val factor = docs[0].getDouble("carbon_footprint")
                if (factor == null) {
                    val localEstimate = calculateLocalEstimate(type, distance)
                    onResult("Estimated emissions: $localEstimate kg CO2e (offline fallback)")
                    return@addOnCompleteListener
                }

                val result = distance * factor
                onResult("Estimated emissions: $result kg CO2e (fallback)")
            }
    }

    private fun calculateLocalEstimate(type: String, distance: Double): Double {
        val factor = localTransportFactors[type] ?: 0.192
        return distance * factor
    }
}
