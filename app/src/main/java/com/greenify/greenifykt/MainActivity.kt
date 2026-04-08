package com.greenify.greenifykt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set the content view to your activity_main.xml layout

        // Check if the user is authenticated. If not, open the login page.
        auth = FirebaseAuth.getInstance()
        user = auth?.currentUser

        if (user == null) {
            // User is not authenticated, open the login page
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Finish the current activity so the user cannot navigate back without logging in
        }

        // Find the GridLayout
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)

        // Find the LinearLayout with id btnCalculator1 inside the GridLayout
        val calculatorLayout: LinearLayout = gridLayout.findViewById(R.id.btnCalculator1)

        // Find the ImageView inside the LinearLayout
        val calculatorImageView: ImageView = calculatorLayout.findViewById(R.id.CalculatorImageView)

        // Find the logout button
        val logoutButton = findViewById<Button>(R.id.logoutbutton)

        // Set click listeners for both the LinearLayout and ImageView
        calculatorLayout.setOnClickListener {
            openCalculatorActivity()
        }

        calculatorImageView.setOnClickListener {
            openCalculatorActivity()
        }

        // Set a click listener for the logout button
        logoutButton.setOnClickListener {
            // Perform logout here
            auth?.signOut()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Finish the current activity
        }

        // Add similar code for other views if needed
    }

    private fun openCalculatorActivity() {
        val intent = Intent(this, Calculator1::class.java)
        startActivity(intent)
    }
}
