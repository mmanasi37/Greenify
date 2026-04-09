package com.greenify.greenifykt

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.HashMap

class Calculator2Electricity : AppCompatActivity() {
    private var edtTypeOfAppliance: EditText? = null
    private var edtTimeUsage: EditText? = null
    private var txtCarbonFootprint: TextView? = null
    private var btnGenerateTips: Button? = null

    // Initialize Firestore
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator2_electricity)

        // Initialize your UI elements
        edtTypeOfAppliance = findViewById(R.id.editTextElectricalAppliance)
        edtTimeUsage = findViewById(R.id.editTextTimeSpent)
        txtCarbonFootprint = findViewById(R.id.editTextCarbonDisplay)

        // Initialize the "Generate Tips" button
        btnGenerateTips = findViewById(R.id.btnGenerateTips)

        // Set a click listener for the "Generate Tips" button
        btnGenerateTips?.setOnClickListener {
            try {
                // Generate and display an eco-friendly tip
                generateEcoFriendlyTip()
            } catch (e: Exception) {
                // Handle exceptions here, e.g., if tip generation fails
                txtCarbonFootprint?.text = "Error generating eco-friendly tip."
            }
        }

        val btnEleCalculation = findViewById<Button>(R.id.btnElectricityCalculator)
        btnEleCalculation.setOnClickListener {
            try {
                // Get the user inputs as strings
                val typeOfApplianceStr = edtTypeOfAppliance?.text.toString()
                val timeSpentStr = edtTimeUsage?.text.toString()

                // Check if both fields are not empty
                if (typeOfApplianceStr.isNotEmpty() && timeSpentStr.isNotEmpty()) {
                    // Parse the input strings to double
                    val timeSpent = timeSpentStr.toDouble()

                    // Fetch the carbon footprint value from Firestore
                    fetchCarbonFootprintFromFirestore(typeOfApplianceStr.toLowerCase(), timeSpent)
                } else {
                    // Handle case where either field is empty
                    txtCarbonFootprint?.text = "Please enter both appliance and time spent using it."
                }
            } catch (e: Exception) {
                // Handle exceptions here, e.g., if calculation fails
                txtCarbonFootprint?.text = "Error calculating carbon footprint."
            }
        }
    }

    // Fetch carbon footprint value from Firestore
    private fun fetchCarbonFootprintFromFirestore(typeOfAppliance: String, timeSpent: Double) {
        // Reference to the "appliances" collection in Firestore
        val appliancesCollection = db.collection("appliances")

        // Query the Firestore database for the specific type of appliance
        val query = appliancesCollection.whereEqualTo("name", typeOfAppliance)

        // Execute the query
        query.get().addOnCompleteListener(OnCompleteListener { task: Task<*> ->
            if (task.isSuccessful) {
                // Query was successful, get the documents
                val querySnapshot = task.result as? QuerySnapshot
                val documents = querySnapshot?.documents

                if (documents != null && documents.isNotEmpty()) {
                    // Retrieve the carbon footprint value from the Firestore document
                    val carbonFootprint = documents[0].getDouble("carbon_footprint")

                    if (carbonFootprint != null) {
                        // Calculate the carbon footprint based on the retrieved value
                        val result = timeSpent * carbonFootprint * 1.0e-3
                        txtCarbonFootprint?.text = "The carbon footprint of the energy consumption is $result kilograms of CO2e."
                    } else {
                        txtCarbonFootprint?.text = "Carbon footprint data not found for $typeOfAppliance."
                    }
                } else {
                    txtCarbonFootprint?.text = "Data not found for $typeOfAppliance."
                }
            } else {
                // Handle query errors here
                txtCarbonFootprint?.text = "Error querying data from Firestore."
            }
        })
    }

    // Generate an eco-friendly tip (you can replace this with your logic)
    private fun generateEcoFriendlyTip() {
        // Reference to the "tips" collection in Firestore
        val tipsCollection = db.collection("tips")

        // Query a random tip from Firestore
        tipsCollection.get().addOnCompleteListener(OnCompleteListener { task: Task<*> ->
            if (task.isSuccessful) {
                // Query was successful, get the documents
                val querySnapshot = task.result as? QuerySnapshot
                val documents = querySnapshot?.documents

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
