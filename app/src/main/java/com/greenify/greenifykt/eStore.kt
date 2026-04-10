package com.greenify.greenifykt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ListenerRegistration
import coil.compose.AsyncImage
import java.util.Locale

private data class StoreProduct(
    val id: String,
    val name: String,
    val category: String,
    val priceKina: Double,
    val ecoScore: Int,
    val co2ReductionKg: Double,
    val imageUrl: String,
    val description: String
)

private data class NewProductInput(
    val name: String,
    val category: String,
    val description: String,
    val priceKina: Double,
    val ecoScore: Int,
    val co2ReductionKg: Double,
    val imageUrl: String
)

private fun fallbackCatalog(): List<StoreProduct> = listOf(
    StoreProduct(
        id = "bottle_01",
        name = "Stainless Steel Bottle",
        category = "Daily Use",
        priceKina = 52.99,
        ecoScore = 92,
        co2ReductionKg = 2.4,
        imageUrl = "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=1200&q=80",
        description = "Reusable 750ml bottle with insulated walls."
    ),
    StoreProduct(
        id = "bag_01",
        name = "Organic Cotton Tote",
        category = "Daily Use",
        priceKina = 29.90,
        ecoScore = 88,
        co2ReductionKg = 1.1,
        imageUrl = "https://images.unsplash.com/photo-1597484661643-2f5fef640dd1?auto=format&fit=crop&w=1200&q=80",
        description = "Lightweight carry bag replacing single-use plastic bags."
    ),
    StoreProduct(
        id = "kitchen_01",
        name = "Bamboo Cutlery Set",
        category = "Kitchen",
        priceKina = 39.50,
        ecoScore = 85,
        co2ReductionKg = 1.8,
        imageUrl = "https://images.unsplash.com/photo-1598965402089-897ce52e8355?auto=format&fit=crop&w=1200&q=80",
        description = "Portable fork, spoon, knife and straw in a fabric case."
    ),
    StoreProduct(
        id = "home_01",
        name = "Compost Bin Starter",
        category = "Home",
        priceKina = 85.00,
        ecoScore = 90,
        co2ReductionKg = 4.6,
        imageUrl = "https://images.unsplash.com/photo-1604186838309-c6715f0d3f61?auto=format&fit=crop&w=1200&q=80",
        description = "Countertop compost setup with charcoal odor filter."
    ),
    StoreProduct(
        id = "energy_01",
        name = "Smart Plug Duo",
        category = "Energy",
        priceKina = 69.00,
        ecoScore = 87,
        co2ReductionKg = 3.2,
        imageUrl = "https://images.unsplash.com/photo-1558002038-1055e2dae1d7?auto=format&fit=crop&w=1200&q=80",
        description = "Track and schedule appliances to reduce phantom power."
    ),
    StoreProduct(
        id = "care_01",
        name = "Refillable Soap Dispenser",
        category = "Home",
        priceKina = 41.20,
        ecoScore = 82,
        co2ReductionKg = 1.4,
        imageUrl = "https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=1200&q=80",
        description = "Glass dispenser kit designed for refill stations."
    ),
    StoreProduct(
        id = "cocoa_01",
        name = "Cocoa Seedlings Pack",
        category = "Agriculture",
        priceKina = 57.00,
        ecoScore = 91,
        co2ReductionKg = 6.8,
        imageUrl = "https://images.unsplash.com/photo-1596591868231-05e44d548f32?auto=format&fit=crop&w=1200&q=80",
        description = "Healthy cocoa seedlings for smallholder farm planting."
    )
)

private fun formatKina(price: Double): String = "K${String.format(Locale.US, "%.2f", price)}"

class eStore : ComponentActivity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val adminUids = setOf("2NjIR8ofydRFvqZzlYK3JYBDPkS2")

    private fun isAdminUser(): Boolean = auth.currentUser?.uid in adminUids

    private fun mapCatalog(snapshot: QuerySnapshot?): List<StoreProduct> {
        return snapshot?.documents
            ?.mapNotNull { doc ->
                val name = doc.getString("name")?.trim().orEmpty()
                val category = doc.getString("category")?.trim().orEmpty()
                val description = doc.getString("description")?.trim().orEmpty()
                if (name.isBlank() || category.isBlank() || description.isBlank()) return@mapNotNull null

                val price = doc.getDouble("price_k") ?: doc.getDouble("price") ?: 0.0
                if (price <= 0.0) return@mapNotNull null

                StoreProduct(
                    id = doc.id,
                    name = name,
                    category = category,
                    priceKina = price,
                    ecoScore = (doc.getLong("eco_score") ?: 75L).toInt(),
                    co2ReductionKg = doc.getDouble("co2_reduction_kg") ?: 0.5,
                    imageUrl = doc.getString("image_url")?.trim().orEmpty(),
                    description = description
                )
            }
            .orEmpty()
    }

    private fun loadCatalog(onDone: (List<StoreProduct>, Boolean, String?) -> Unit) {
        db.collection("estore_products").get().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onDone(fallbackCatalog(), true, task.exception?.localizedMessage)
                return@addOnCompleteListener
            }

            val mapped = mapCatalog(task.result as? QuerySnapshot)
            if (mapped.isEmpty()) {
                onDone(fallbackCatalog(), true, "No valid live products found.")
                return@addOnCompleteListener
            }

            onDone(mapped, false, null)
        }
    }

    private fun observeCatalog(onDone: (List<StoreProduct>, Boolean, String?) -> Unit): ListenerRegistration {
        return db.collection("estore_products").addSnapshotListener { snapshot, error ->
            if (error != null) {
                onDone(fallbackCatalog(), true, error.localizedMessage)
                return@addSnapshotListener
            }

            val mapped = mapCatalog(snapshot)
            if (mapped.isEmpty()) {
                onDone(fallbackCatalog(), true, "No valid live products found.")
                return@addSnapshotListener
            }

            onDone(mapped, false, null)
        }
    }

    private fun seedCatalog(onDone: (Boolean, Int, String?) -> Unit) {
        val seedData = fallbackCatalog()
        db.runBatch { batch ->
            seedData.forEach { product ->
                val payload = hashMapOf(
                    "name" to product.name,
                    "category" to product.category,
                    "description" to product.description,
                    "price_k" to product.priceKina,
                    "eco_score" to product.ecoScore,
                    "co2_reduction_kg" to product.co2ReductionKg,
                    "image_url" to product.imageUrl,
                    "updated_at_ms" to System.currentTimeMillis()
                )
                batch.set(db.collection("estore_products").document(product.id), payload)
            }
        }.addOnSuccessListener {
            onDone(true, seedData.size, null)
        }.addOnFailureListener { err ->
            onDone(false, 0, err.localizedMessage)
        }
    }

    private fun addProduct(input: NewProductInput, onDone: (Boolean, String?) -> Unit) {
        val base = input.name.trim().lowercase(Locale.US)
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
        val docId = if (base.isBlank()) {
            "item_${System.currentTimeMillis()}"
        } else {
            "${base}_${System.currentTimeMillis()}"
        }

        val payload = hashMapOf(
            "name" to input.name,
            "category" to input.category,
            "description" to input.description,
            "price_k" to input.priceKina,
            "eco_score" to input.ecoScore,
            "co2_reduction_kg" to input.co2ReductionKg,
            "image_url" to input.imageUrl,
            "updated_at_ms" to System.currentTimeMillis()
        )

        db.collection("estore_products").document(docId).set(payload)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { err -> onDone(false, err.localizedMessage) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeModeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContent {
            val settingsPrefs = getSharedPreferences("greenify_settings", MODE_PRIVATE)
            var darkMode by rememberSaveable { mutableStateOf(ThemeModeManager.isDarkModeEnabled(this)) }
            val cartIds = remember { mutableStateListOf<String>() }
            var products by remember { mutableStateOf(fallbackCatalog()) }
            var statusMessage by rememberSaveable { mutableStateOf("Loading products...") }
            val autoRefreshStore = remember { settingsPrefs.getBoolean("estore_auto_refresh", true) }
            val showImpactInKg = remember { settingsPrefs.getBoolean("impact_in_kg", true) }
            val app = remember { FirebaseApp.initializeApp(this) ?: FirebaseApp.getInstance() }
            val authInfo = remember(auth.currentUser?.uid, auth.currentUser?.email) {
                val user = auth.currentUser
                if (user == null) {
                    "Auth: Not signed in"
                } else {
                    "Auth UID: ${user.uid}"
                }
            }
            val isAdmin = remember(auth.currentUser?.uid) { isAdminUser() }
            val firebaseInfo = remember(app.options.projectId, app.options.applicationId) {
                "Firebase: ${app.options.projectId} (${app.options.applicationId})"
            }

            fun refreshCatalog() {
                statusMessage = "Refreshing catalog..."
                loadCatalog { loaded, fallback, error ->
                    products = loaded
                    statusMessage = if (fallback) {
                        "Showing offline catalog. ${error ?: ""}".trim()
                    } else {
                        "Live catalog updated."
                    }
                }
            }

            fun seedCatalogNow() {
                statusMessage = "Seeding catalog to Firestore..."
                seedCatalog { success, count, error ->
                    if (!success) {
                        statusMessage = "Seed failed: ${error ?: "unknown error"}"
                        return@seedCatalog
                    }
                    statusMessage = "Seeded $count products to Firestore."
                    refreshCatalog()
                }
            }

            LaunchedEffect(Unit) {
                refreshCatalog()
            }

            DisposableEffect(Unit) {
                if (!autoRefreshStore) {
                    statusMessage = "Live sync is disabled in Settings. Tap Refresh to load products."
                    onDispose { }
                } else {
                    val registration = observeCatalog { loaded, fallback, error ->
                        products = loaded
                        statusMessage = if (fallback) {
                            "Showing offline catalog. ${error ?: ""}".trim()
                        } else {
                            "Live catalog synced."
                        }
                    }

                    onDispose {
                        registration.remove()
                    }
                }
            }

            GreenifyCalculatorTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EStoreScreen(
                        darkMode = darkMode,
                        products = products,
                        cartIds = cartIds,
                        showImpactInKg = showImpactInKg,
                        statusMessage = statusMessage,
                        authInfo = authInfo,
                        isAdmin = isAdmin,
                        firebaseInfo = firebaseInfo,
                        onBack = { finish() },
                        onToggleDarkMode = {
                            darkMode = it
                            ThemeModeManager.setDarkMode(this, it)
                        },
                        onRefreshCatalog = { refreshCatalog() },
                        onSeedCatalog = { seedCatalogNow() },
                        onAddProduct = { input, onDone ->
                            statusMessage = "Adding product..."
                            addProduct(input) { success, error ->
                                statusMessage = if (success) {
                                    "Product added to live catalog."
                                } else {
                                    "Add failed: ${error ?: "unknown error"}"
                                }
                                onDone(success, error)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EStoreScreen(
    darkMode: Boolean,
    products: List<StoreProduct>,
    cartIds: MutableList<String>,
    showImpactInKg: Boolean,
    statusMessage: String,
    authInfo: String,
    isAdmin: Boolean,
    firebaseInfo: String,
    onBack: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onRefreshCatalog: () -> Unit,
    onSeedCatalog: () -> Unit,
    onAddProduct: (NewProductInput, (Boolean, String?) -> Unit) -> Unit
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    val categories = remember(products) { listOf("All") + products.map { it.category }.distinct() }
    val filteredProducts = remember(products, searchText, selectedCategory) {
        products.filter { product ->
            val categoryMatch = selectedCategory == "All" || product.category == selectedCategory
            val textMatch = searchText.isBlank() ||
                product.name.contains(searchText, ignoreCase = true) ||
                product.description.contains(searchText, ignoreCase = true)
            categoryMatch && textMatch
        }
    }

    val cartProducts = products.filter { it.id in cartIds }
    val total = cartProducts.sumOf { it.priceKina }
    val totalCo2 = cartProducts.sumOf { it.co2ReductionKg }

    val shellColor = if (darkMode) Color(0xFF1E222B) else Color(0xFFF4F7FB)
    val barColor = if (darkMode) Color(0xFF264A7E) else Color(0xFF4267B2)
    val cardColor = if (darkMode) Color(0xFF2A303C) else Color.White
    val borderColor = if (darkMode) Color(0xFF3B4454) else Color(0xFFD9DFEA)
    val textColor = if (darkMode) Color(0xFFF3F6FB) else Color(0xFF1A1F2B)
    val mutedColor = if (darkMode) Color(0xFFB8C2D4) else Color(0xFF5B6578)

    Column(modifier = Modifier.fillMaxSize().background(shellColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("Back", color = Color.White) }
                Text("greenify eStore", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            TextButton(onClick = { onToggleDarkMode(!darkMode) }) {
                Text(if (darkMode) "Light" else "Dark", color = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CartSummaryCard(
                    itemCount = cartIds.size,
                    totalKina = total,
                    totalCo2 = totalCo2,
                    showImpactInKg = showImpactInKg,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    mutedColor = mutedColor
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(statusMessage, color = mutedColor, style = MaterialTheme.typography.bodySmall)
                            Text(authInfo, color = mutedColor, style = MaterialTheme.typography.labelSmall)
                            Text(firebaseInfo, color = mutedColor, style = MaterialTheme.typography.labelSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = onRefreshCatalog) { Text("Refresh") }
                            TextButton(onClick = onSeedCatalog) { Text("Seed") }
                        }
                    }
                }
            }

            item {
                SearchFilterCard(
                    searchText = searchText,
                    selectedCategory = selectedCategory,
                    categories = categories,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textColor = textColor,
                    onSearchChange = { searchText = it },
                    onCategoryChange = { selectedCategory = it }
                )
            }

            if (isAdmin) {
                item {
                    AdminAddProductCard(
                        cardColor = cardColor,
                        borderColor = borderColor,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        onAddProduct = onAddProduct
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Text(
                            text = "No products match your filters.",
                            modifier = Modifier.padding(16.dp),
                            color = textColor
                        )
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        inCart = product.id in cartIds,
                        showImpactInKg = showImpactInKg,
                        cardColor = cardColor,
                        borderColor = borderColor,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        onToggleCart = {
                            if (product.id in cartIds) cartIds.remove(product.id) else cartIds.add(product.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAddProductCard(
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    mutedColor: Color,
    onAddProduct: (NewProductInput, (Boolean, String?) -> Unit) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var ecoScore by rememberSaveable { mutableStateOf("") }
    var co2Reduction by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var localMessage by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Admin: Add Product", color = textColor, fontWeight = FontWeight.Bold)
            Text("Create a product directly in Firestore estore_products.", color = mutedColor, style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product name") }, modifier = Modifier.fillMaxWidth(), maxLines = 1)
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), maxLines = 1)
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (K)") }, modifier = Modifier.fillMaxWidth(), maxLines = 1)
            OutlinedTextField(value = ecoScore, onValueChange = { ecoScore = it }, label = { Text("Eco score (0-100)") }, modifier = Modifier.fillMaxWidth(), maxLines = 1)
            OutlinedTextField(value = co2Reduction, onValueChange = { co2Reduction = it }, label = { Text("CO2 reduction (kg)") }, modifier = Modifier.fillMaxWidth(), maxLines = 1)
            OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL") }, modifier = Modifier.fillMaxWidth(), maxLines = 1)

            Button(
                onClick = {
                    val parsedPrice = price.trim().toDoubleOrNull()
                    val parsedEco = ecoScore.trim().toIntOrNull()
                    val parsedCo2 = co2Reduction.trim().toDoubleOrNull()

                    if (name.trim().isBlank() || category.trim().isBlank() || description.trim().isBlank()) {
                        localMessage = "Fill in name, category and description."
                        return@Button
                    }
                    if (parsedPrice == null || parsedPrice <= 0.0) {
                        localMessage = "Enter a valid price in Kina."
                        return@Button
                    }
                    if (parsedEco == null || parsedEco !in 0..100) {
                        localMessage = "Eco score must be between 0 and 100."
                        return@Button
                    }
                    if (parsedCo2 == null || parsedCo2 < 0.0) {
                        localMessage = "Enter a valid CO2 reduction value."
                        return@Button
                    }

                    val input = NewProductInput(
                        name = name.trim(),
                        category = category.trim(),
                        description = description.trim(),
                        priceKina = parsedPrice,
                        ecoScore = parsedEco,
                        co2ReductionKg = parsedCo2,
                        imageUrl = imageUrl.trim()
                    )

                    onAddProduct(input) { success, error ->
                        localMessage = if (success) {
                            name = ""
                            category = ""
                            description = ""
                            price = ""
                            ecoScore = ""
                            co2Reduction = ""
                            imageUrl = ""
                            "Product added."
                        } else {
                            "Add failed: ${error ?: "unknown error"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
                shape = RoundedCornerShape(7.dp)
            ) {
                Text("Add Product")
            }

            if (localMessage.isNotBlank()) {
                Text(localMessage, color = mutedColor, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CartSummaryCard(
    itemCount: Int,
    totalKina: Double,
    totalCo2: Double,
    showImpactInKg: Boolean,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    mutedColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cart Overview", color = textColor, fontWeight = FontWeight.Bold)
            Text("$itemCount items selected", color = mutedColor)
            Text("Total: ${formatKina(totalKina)}", color = textColor, fontWeight = FontWeight.SemiBold)
            Text(
                if (showImpactInKg) {
                    "Potential CO2 reduction: ${String.format(Locale.US, "%.1f", totalCo2)} kg"
                } else {
                    "Potential eco impact score: ${(totalCo2 * 10).toInt()}"
                },
                color = mutedColor
            )
        }
    }
}

@Composable
private fun SearchFilterCard(
    searchText: String,
    selectedCategory: String,
    categories: List<String>,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Find products", color = textColor, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchChange,
                label = { Text("Search eco products") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                categories.take(4).forEach { category ->
                    CategoryChip(
                        text = category,
                        selected = category == selectedCategory,
                        onClick = { onCategoryChange(category) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                categories.drop(4).forEach { category ->
                    CategoryChip(
                        text = category,
                        selected = category == selectedCategory,
                        onClick = { onCategoryChange(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFF4267B2) else Color.Transparent
    val fg = if (selected) Color.White else Color(0xFF4267B2)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF4267B2), RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ProductCard(
    product: StoreProduct,
    inCart: Boolean,
    showImpactInKg: Boolean,
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
    mutedColor: Color,
    onToggleCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .background(Color(0xFFE8EDF6)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(product.name, color = textColor, fontWeight = FontWeight.SemiBold)
                    Text(formatKina(product.priceKina), color = textColor, fontWeight = FontWeight.Bold)
                }

                Text(product.description, color = mutedColor, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (showImpactInKg) {
                        "${product.category} • Eco Score ${product.ecoScore} • -${String.format(Locale.US, "%.1f", product.co2ReductionKg)} kg CO2"
                    } else {
                        "${product.category} • Eco Score ${product.ecoScore} • Impact ${(product.co2ReductionKg * 10).toInt()}"
                    },
                    color = mutedColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Button(
                    onClick = onToggleCart,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inCart) Color(0xFF2E7D32) else Color(0xFF4267B2),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, if (inCart) Color(0xFF2E7D32) else Color(0xFF4267B2))
                ) {
                    Text(
                        if (inCart) "In Cart - Tap to Remove" else "Add to Cart",
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}