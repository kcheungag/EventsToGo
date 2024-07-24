package com.example.eventstogo_group6.screens

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.eventstogo_group6.database.UserRepository
import com.example.eventstogo_group6.databinding.ActivityUserAccountBinding
import com.example.eventstogo_group6.enums.SharedPrefRef
import com.example.eventstogo_group6.models.User

class UserAccountActivity : AppCompatActivity() {

    private val TAG: String = this@UserAccountActivity.toString()
    private lateinit var binding: ActivityUserAccountBinding
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var userRepository: UserRepository
    private var currentUser : String = ""
    private lateinit var userFromDB : User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(SharedPrefRef.SHARED_PREF_NAME.value, MODE_PRIVATE)
        userRepository = UserRepository(applicationContext)
        getUserDetails()
        binding.tvError.setText("")
        binding.btnUpdateUser.setOnClickListener {
            updateUser()
        }
    }

    private fun getUserDetails(){
        currentUser = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "").toString()
        Log.d(TAG, "getUserDetails: Login status: $currentUser")
        if (currentUser!="") {
            userRepository.getUserFromDB(currentUser).observe(this) { user ->
                if (user != null) {
                    userFromDB = user
                    // Populate the UI with event details
                    Log.d(TAG, "getUserDetails: ${user.toString()}")
                    binding.tvEmail.setText(user.email)
                    binding.etName.setText(user.name)
                    binding.etCity.setText(user.city)
                    if(user.role=="Organizer")
                        binding.cbRole.isChecked=true
                } else {
                    Log.d(TAG, "User document does not exist or an error occurred for ID: $currentUser")
                }
            }
        }
    }

    private fun updateUser(){
        this.binding.tvError.setText("")
        val name = this.binding.etName.text.toString()
        val city = this.binding.etCity.text.toString()
        if(name.isNullOrEmpty() || city.isNullOrEmpty()) {
            this.binding.tvError.setText("Error: All fields must be filled in!")
            return@updateUser
        }
        var role = "User"
        if (this.binding.cbRole.isChecked)
            role = "Organizer"
        val updatedUser = User(userFromDB.email, userFromDB.password, name, city, role)
        this.userRepository.updateUser(updatedUser)
        Log.d(TAG, "updateUser: User account successfully updated with email ${userFromDB.email}")
        Toast.makeText(this, "ACCOUNT UPDATED SUCCESSFULLY", Toast.LENGTH_SHORT).show()
        finish()
        startActivity(Intent(this, UserAccountActivity::class.java))
    }
}