package com.greenify.greenifykt

import android.R
import android.content.Intent
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
import com.greenify.greenifykt.MainActivity
import com.greenify.greenifykt.Registration

class Login : AppCompatActivity() {
    var editTextEmail: TextInputEditText? = null
    var editTextPassword: TextInputEditText? = null
    var email: String? = null
    var password: String? = null
    var textViewError: TextView? = null
    var textViewLogin: TextView? = null
    var buttonSubmitLogin: Button? = null
    var progressBar: ProgressBar? = null
    var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.greenify.greenifykt.R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()


        editTextEmail = findViewById(com.greenify.greenifykt.R.id.email)
        editTextPassword = findViewById(com.greenify.greenifykt.R.id.password)
        textViewError = findViewById(com.greenify.greenifykt.R.id.error)
        textViewLogin = findViewById(com.greenify.greenifykt.R.id.registerNow)
        buttonSubmitLogin = findViewById(com.greenify.greenifykt.R.id.submit)
        progressBar = findViewById(com.greenify.greenifykt.R.id.loading)


        textViewLogin?.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent(
                applicationContext,
                Registration::class.java
            )
            startActivity(intent)
            finish()
        })
        buttonSubmitLogin?.setOnClickListener(View.OnClickListener setOnClickListener@{ view: View? ->
            progressBar?.setVisibility(View.VISIBLE)
            textViewError?.setVisibility(View.GONE)
            email = editTextEmail?.getText().toString()
            password = editTextPassword?.getText().toString()
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@Login, "Enter Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this@Login, "Enter Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mAuth!!.signInWithEmailAndPassword(email!!, password!!)
                .addOnCompleteListener { task ->
                    progressBar?.setVisibility(View.GONE)
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@Login,
                            "Authentication Successful.",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent =
                            Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            this@Login, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        })
    }
}