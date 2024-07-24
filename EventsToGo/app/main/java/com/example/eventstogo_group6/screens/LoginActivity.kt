package com.example.eventstogo_group6.screens

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.eventstogo_group6.databinding.ActivityLoginBinding
import com.example.eventstogo_group6.enums.SharedPrefRef
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    lateinit var sharedPreferences: SharedPreferences
    lateinit var prefEditor: SharedPreferences.Editor
    private  lateinit var firebaseAuth : FirebaseAuth
    private val TAG: String = this@LoginActivity.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = applicationContext.getSharedPreferences(SharedPrefRef.SHARED_PREF_NAME.value, MODE_PRIVATE)
        prefEditor = sharedPreferences.edit()

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun loginUser(){
        this.binding.tvError.setText("")
        val email = this.binding.etEmail.text.toString()
        val password = this.binding.etPassword.text.toString()
        if(email.isNullOrEmpty() || password.isNullOrEmpty()){
            this.binding.tvError.setText("Error: All fields must be filled in!")
            return@loginUser
        }
        this.firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){task ->
                if (task.isSuccessful){
                    Log.d(TAG, "loginUser: Login successful")
                    Toast.makeText(this, "LOGGED IN SUCCESSFULLY", Toast.LENGTH_SHORT).show()
                    saveToPrefs(email)
                    finish()
                }else{
                    Log.e(TAG, "loginUser: Login Failed : ${task.exception}", )
                    this.binding.tvError.setText("Authentication failed. Check the credentials")
                }
            }
    }

    private fun saveToPrefs(email : String){
        prefEditor.putString(SharedPrefRef.CURRENT_USER.value, email).apply()
    }
}