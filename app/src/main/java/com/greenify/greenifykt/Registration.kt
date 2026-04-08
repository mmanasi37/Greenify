package com.greenify.greenifykt
//noinspection SuspiciousImport
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth


class Registration : AppCompatActivity() {
    private var editTextEmail: TextInputEditText? = null
    private var editTextPassword: TextInputEditText? = null
    private var email: String? = null
    private var password: String? = null
    private var textViewError: TextView? = null
    private var textViewLogin: TextView? = null
    private var buttonSubmit: Button? = null
    private var progressBar: ProgressBar? = null
    private var mAuth: FirebaseAuth? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_registration)
        mAuth = FirebaseAuth.getInstance()
        // editTextName = findViewById(R.id.name);



        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        textViewError = findViewById(R.id.error)
        textViewLogin = findViewById(R.id.loginNow)
        buttonSubmit = findViewById(R.id.submit)
        progressBar = findViewById(R.id.loading)


        textViewLogin?.setOnClickListener {
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
        }
        buttonSubmit?.setOnClickListener {
            //name = editTextName.getText().toString();
            email = editTextEmail?.text.toString()
            password = editTextPassword?.text.toString()
            mAuth!!.createUserWithEmailAndPassword(email!!, password!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@Registration, "Registration Successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(applicationContext, Login::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        val errorMessage = task.exception!!.message
                        Toast.makeText(
                            this@Registration,
                            "Authentication failed: $errorMessage", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}