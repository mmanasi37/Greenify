import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class Calculator4Transport : AppCompatActivity() {
    private var txtCarbonFootprint: TextView? = null
    private var btnGenerateTips: Button? = null

    // Initialize Firestore
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator4_transport)

        // Initialize your UI elements
        txtCarbonFootprint = findViewById(R.id.editTextCarbonDisplay)

        // Initialize the "Generate Tips" button
        btnGenerateTips = findViewById(R.id.btnGenerateTransportTips)

        // Set a click listener for the "Generate Tips" button
        btnGenerateTips?.setOnClickListener {
            // Show prompts for input
            showTransportInputDialog()
        }
    }

    // Show prompt for entering type of transport
    private fun showTransportInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Type of Transport")

        val view = layoutInflater.inflate(R.layout.dialog_input_transport, null)
        val inputTypeOfTransport = view.findViewById<EditText>(R.id.inputTypeOfTransport)

        builder.setView(view)

        builder.setPositiveButton("Next") { dialog, _ ->
            val typeOfTransportStr = inputTypeOfTransport.text.toString()

            // Check if the field is not empty
            if (typeOfTransportStr.isNotEmpty()) {
                // Show the distance input prompt
                dialog.dismiss()
                showDistanceInputDialog(typeOfTransportStr)
            } else {
                // Handle case where the field is empty
                txtCarbonFootprint?.text = "Please enter the type of transport."
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // Show prompt for entering distance traveled
    private fun showDistanceInputDialog(typeOfTransport: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Distance Traveled")

        val view = layoutInflater.inflate(R.layout.dialog_input_transport, null)
        val inputDistance = view.findViewById<EditText>(R.id.inputTypeOfTransport)
        inputDistance.hint = "Distance (in km)"

        builder.setView(view)

        builder.setPositiveButton("Calculate") { dialog, _ ->
            val distanceTravelledStr = inputDistance.text.toString()

            // Check if the field is not empty
            if (distanceTravelledStr.isNotEmpty()) {
                // Parse the input string to double
                val distanceTravelled = distanceTravelledStr.toDouble()

                // Fetch the carbon footprint value from Firestore
                fetchCarbonFootprintFromFirestore(typeOfTransport.toLowerCase(Locale.getDefault()), distanceTravelled)
            } else {
                // Handle case where the field is empty
                txtCarbonFootprint?.text = "Please enter the distance traveled."
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // Fetch carbon footprint value from Firestore
    private fun fetchCarbonFootprintFromFirestore(typeOfTransport: String, distanceTravelled: Double) {
        // Reference to the "transport" collection in Firestore
        val transportCollection = db.collection("transport")

        // Query the Firestore database for the specific type of transport
        val query = transportCollection.whereEqualTo("name", typeOfTransport)

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
                        val result = distanceTravelled * carbonFootprint
                        txtCarbonFootprint?.text = "The carbon footprint of the transportation is $result kilograms of CO2e."
                    } else {
                        txtCarbonFootprint?.text = "Carbon footprint data not found for $typeOfTransport."
                    }
                } else {
                    txtCarbonFootprint?.text = "Data not found for $typeOfTransport."
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
