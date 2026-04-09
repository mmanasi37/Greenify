package com.greenify.greenifykt

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {
    private var editTextEmail: TextInputEditText? = null
    private var textViewError: TextView? = null
    private var buttonSendEmail: Button? = null
    private var buttonBackToLogin: Button? = null
    private var progressBar: ProgressBar? = null
    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        mAuth = FirebaseAuth.getInstance()

        editTextEmail = findViewById(R.id.email)
        textViewError = findViewById(R.id.error)
        buttonSendEmail = findViewById(R.id.submit)
        buttonBackToLogin = findViewById(R.id.backToLogin)
        progressBar = findViewById(R.id.loading)

        buttonSendEmail?.setOnClickListener {
            val email = editTextEmail?.text?.toString()?.trim()

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar?.visibility = View.VISIBLE
            textViewError?.visibility = View.GONE

            // Send password reset email
            mAuth?.sendPasswordResetEmail(email!!)
                ?.addOnCompleteListener { task ->
                    progressBar?.visibility = View.GONE

                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset email sent to $email. Check your inbox and spam folder.",
                            Toast.LENGTH_LONG
                        ).show()
                        editTextEmail?.setText("")
                        finish()
                    } else {
                        textViewError?.text = task.exception?.message ?: "Failed to send reset email"
                        textViewError?.visibility = View.VISIBLE
                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        buttonBackToLogin?.setOnClickListener {
            finish()
        }
    }
}
