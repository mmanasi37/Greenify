package com.greenify.greenifykt

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class Calculator1 : AppCompatActivity() {
    private var edtTypeOfMeat: EditText? = null
    private var edtAmountConsumed: EditText? = null
    private var txtCarbonFootprint: TextView? = null
    private var ElectricityCalculator: Button? = null
    private var FoodCalculator: Button? = null
    private var TransportCalculator: Button? = null
    private var btnGenerateTips: Button? = null

    // Initialize Firestore
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator1)

        // Initialize your UI elements
        edtTypeOfMeat = findViewById(R.id.meatconsumed)
        edtAmountConsumed = findViewById(R.id.weightofmeat)
        txtCarbonFootprint = findViewById(R.id.carbondisplay)
        ElectricityCalculator = findViewById(R.id.btngotoElectricity)
        FoodCalculator = findViewById(R.id.btnFoodCalculator)
        TransportCalculator = findViewById(R.id.btnTransport)
        val btnMeatcalculation = findViewById<Button>(R.id.btnCalculateMeat)

        // Initialize the "Generate Tips" button
        btnGenerateTips = findViewById(R.id.btnGenerateTips)

        // Set a click listener for the "Generate Tips" button
        btnGenerateTips?.setOnClickListener {
            // Fetch and display an eco-friendly tip from Firestore
            fetchEcoFriendlyTipFromFirestore()
        }

        //Go the Electricity Calculator
        ElectricityCalculator?.setOnClickListener {
            val intent = Intent(applicationContext, Calculator2Electricity::class.java)
            startActivity(intent)
            finish()
        }

        //Go to Food Calculator
        FoodCalculator?.setOnClickListener {
            val intent = Intent(applicationContext, Calculator3Food::class.java)
            startActivity(intent)
            finish()
        }

        //Go to Transport Calculator
        TransportCalculator?.setOnClickListener {
            val intent = Intent(applicationContext, Calculator4Transport::class.java)
            startActivity(intent)
            finish()
        }

        btnMeatcalculation.setOnClickListener {
            try {
                // Get the user inputs as strings
                val typeOfMeatStr = edtTypeOfMeat?.text.toString()
                val amountConsumedStr = edtAmountConsumed?.text.toString()

                // Check if both fields are not empty
                if (typeOfMeatStr.isNotEmpty() && amountConsumedStr.isNotEmpty()) {
                    // Parse the input strings to double
                    val amountConsumed = amountConsumedStr.toDouble()

                    // Fetch the carbon footprint value from Firestore
                    fetchCarbonFootprintFromFirestore(typeOfMeatStr.toLowerCase(Locale.getDefault()), amountConsumed)
                } else {
                    // Handle case where either field is empty
                    txtCarbonFootprint?.text = "Please enter both type of meat and amount consumed."
                }
            } catch (e: Exception) {
                // Handle exceptions here, e.g., if calculation fails
                txtCarbonFootprint?.text = "Error calculating carbon footprint."
            }
        }
    }

    // Fetch eco-friendly tip from Firestore
    private fun fetchEcoFriendlyTipFromFirestore() {
        // Reference to the "tips" collection in Firestore
        val tipsCollection = db.collection("tips")

        // Query a random tip from Firestore
        tipsCollection.get().addOnCompleteListener(OnCompleteListener { task: Task<*> ->
            if (task.isSuccessful) {
                // Query was successful, get the documents
                val documents = task.result?.documents

                if (documents != null && documents.isNotEmpty()) {
                    // Retrieve a random eco-friendly tip
                    val randomTip = documents.random()
                    val tipText = randomTip.getString("tip")

                    if (tipText != null) {
                        // Display the retrieved tip
                        txtCarbonFootprint?.text = tipText
                    } else {
                        txtCarbonFootprint?.text = "Eco-friendly tip data not found."
                    }
                } else {
                    txtCarbonFootprint?.text = "No eco-friendly tips available."
                }
            } else {
                // Handle query errors here
                txtCarbonFootprint?.text = "Error querying tips from Firestore."
            }
        })
    }

    // Fetch carbon footprint value from Firestore
    private fun fetchCarbonFootprintFromFirestore(typeOfMeat: String, amountConsumed: Double) {
        // Reference to the "foods" collection in Firestore
        val foodsCollection = db.collection("foods")

        // Query the Firestore database for the specific type of meat
        val query = foodsCollection.whereEqualTo("name", typeOfMeat)

        // Execute the query
        query.get().addOnCompleteListener(OnCompleteListener { task: Task<*> ->
            if (task.isSuccessful) {
                // Query was successful, get the documents
                val documents = task.result?.documents

                if (documents != null && documents.isNotEmpty()) {
                    // Retrieve the carbon footprint value from the Firestore document
                    val carbonFootprint = documents[0].getDouble("carbon_footprint")

                    if (carbonFootprint != null) {
                        // Calculate the carbon footprint based on the retrieved value
                        val result = amountConsumed * carbonFootprint
                        txtCarbonFootprint?.text = "The carbon footprint of the meat is $result kilograms of CO2e."
                    } else {
                        txtCarbonFootprint?.text = "Carbon footprint data not found for $typeOfMeat."
                    }
                } else {
                    txtCarbonFootprint?.text = "Data not found for $typeOfMeat."
                }
            } else {
                // Handle query errors here
                txtCarbonFootprint?.text = "Error querying data from Firestore."
            }
        })
    }
    private fun generateEcoFriendlyTip() {
        // Reference to the "tips" collection in Firestore
        val tipsCollection = db.collection("tips")

        // Query a random tip from Firestore
        tipsCollection.get().addOnCompleteListener(OnCompleteListener { task: Task<*> ->
            if (task.isSuccessful) {
                // Query was successful, get the documents
                val documents = task.result?.documents

                if (documents != null && documents.isNotEmpty()) {
                    // Retrieve a random eco-friendly tip
                    val randomTip = documents.random()
                    val tipText = randomTip.getString("tip")

                    if (tipText != null) {
                        // Display the retrieved tip
                        txtCarbonFootprint?.text = tipText
                    } else {
                        txtCarbonFootprint?.text = "Eco-friendly tip data not found."
                    }
                } else {
                    txtCarbonFootprint?.text = "No eco-friendly tips available."
                }
            } else {
                // Handle query errors here
                txtCarbonFootprint?.text = "Error querying tips from Firestore."
            }
        })
    }
}
