package com.greenify.greenifykt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.app.AlertDialog
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.Locale

class Calculator3Food : AppCompatActivity() {

    private var txtCarbonFootprint: TextView? = null
    private var btnGenerateTips: Button? = null

    // Initialize Firestore
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator3_food)

        // Initialize your UI elements
        txtCarbonFootprint = findViewById(R.id.carbondisplay)

        // Initialize the "Generate Tips" button
        btnGenerateTips = findViewById(R.id.btnGenerateFoodTips)

        // Set a click listener for the "Generate Tips" button
        btnGenerateTips?.setOnClickListener {
            // Show prompts for input
            showFoodTypeInputDialog()
        }
    }

    // Show prompt for entering type of food
    private fun showFoodTypeInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Type of Food")

        val view = layoutInflater.inflate(R.layout.dialog_input_food, null)
        val inputTypeOfFood = view.findViewById<EditText>(R.id.inputTypeOfFood)

        builder.setView(view)

        builder.setPositiveButton("Next") { dialog, _ ->
            val typeOfFoodStr = inputTypeOfFood.text.toString()

            // Check if the field is not empty
            if (typeOfFoodStr.isNotEmpty()) {
                // Show the amount input prompt
                dialog.dismiss()
                showAmountInputDialog(typeOfFoodStr)
            } else {
                // Handle case where the field is empty
                txtCarbonFootprint?.text = "Please enter the type of food."
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // Show prompt for entering amount eaten in kg
    private fun showAmountInputDialog(typeOfFood: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Amount Eaten (in kg)")

        val view = layoutInflater.inflate(R.layout.dialog_input_food, null)
        val inputAmountEaten = view.findViewById<EditText>(R.id.inputTypeOfFood)
        inputAmountEaten.hint = "Amount (kg)"

        builder.setView(view)

        builder.setPositiveButton("Calculate") { dialog, _ ->
            val amountEatenStr = inputAmountEaten.text.toString()

            // Check if the field is not empty
            if (amountEatenStr.isNotEmpty()) {
                // Parse the input string to double
                val amountEaten = amountEatenStr.toDouble()

                // Fetch the carbon footprint value from Firestore
                fetchCarbonFootprintFromFirestore(typeOfFood.toLowerCase(Locale.getDefault()), amountEaten)
            } else {
                // Handle case where the field is empty
                txtCarbonFootprint?.text = "Please enter the amount eaten in kilograms."
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // Fetch carbon footprint value from Firestore
    private fun fetchCarbonFootprintFromFirestore(typeOfFood: String, amountEaten: Double) {
        // Reference to the "foods" collection in Firestore
        val foodsCollection = db.collection("foods")

        // Query the Firestore database for the specific type of food
        val query = foodsCollection.whereEqualTo("name", typeOfFood)

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
                        val result = amountEaten * carbonFootprint
                        txtCarbonFootprint?.text = "The carbon footprint of the food is $result kilograms of CO2e."
                    } else {
                        txtCarbonFootprint?.text = "Carbon footprint data not found for $typeOfFood."
                    }
                } else {
                    txtCarbonFootprint?.text = "Data not found for $typeOfFood."
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
