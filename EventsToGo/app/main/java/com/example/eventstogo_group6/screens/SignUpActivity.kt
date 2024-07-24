package com.example.eventstogo_group6.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.eventstogo_group6.api.CityInterface
import com.example.eventstogo_group6.api.RetrofitInstance
import com.example.eventstogo_group6.databinding.ActivitySignUpBinding
import com.example.eventstogo_group6.models.City
import com.example.eventstogo_group6.models.User
import com.example.eventstogo_group6.database.UserRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val TAG: String = this@SignUpActivity.toString()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var countryCode : String
    val cityList : MutableList<String> = mutableListOf()
    private  lateinit var firebaseAuth : FirebaseAuth
    private lateinit var userRepository: UserRepository

    private val APP_PERMISSIONS_LIST = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        countryCode = "CA"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        multiplePermissionsResultLauncher.launch(APP_PERMISSIONS_LIST)
        this.firebaseAuth = FirebaseAuth.getInstance()
        this.userRepository = UserRepository(applicationContext)

        var api : CityInterface = RetrofitInstance.retrofitService
        //launches a background task
        lifecycleScope.launch {
            val cities : List<City> = api.getCities(countryCode)
            for(city in cities)
                cityList.add(city.name)
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(applicationContext, android.R.layout.simple_list_item_1, cityList)
            adapter.notifyDataSetChanged()
            binding.spnCity.adapter=adapter
        }


        binding.btnSignup.setOnClickListener {
            signUpUser()
        }
    }

    private val multiplePermissionsResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultsList ->
            Log.d(TAG, resultsList.toString())

            var allPermissionsGrantedTracker = true

            for (item in resultsList.entries) {
                if (item.key in APP_PERMISSIONS_LIST && item.value == false) {
                    allPermissionsGrantedTracker = false
                }
            }

            if (allPermissionsGrantedTracker) {
                Log.d(TAG,"All permissions granted")
                getCurrentLocationCountry()
            } else {
                Log.d(TAG,"Insufficient Permissions")
            }
        }

    private fun getCurrentLocationCountry(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        Log.d(TAG, "Location is null")
                        return@addOnSuccessListener
                    }

                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        if (addresses.isNullOrEmpty()) {
                            Log.e(TAG, "Geocoding failed. No addresses found.")
                            // Handle the case where no addresses are found
                            return@addOnSuccessListener
                        }

                        val country = addresses[0]?.countryCode
                        if (country != null) {
                            Log.d(TAG, "Country code: $country")
                            countryCode = country
                        } else {
                            Log.e(TAG, "Country code is null")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error during geocoding: ${e.message}")
                        // Handle the exception, for example, show a default location or error message
                    }
                }
        }
    }

    private fun signUpUser(){
        this.binding.tvError.setText("")
        val name = this.binding.etName.text.toString()
        val email = this.binding.etEmail.text.toString()
        val password = this.binding.etPassword.text.toString()
        if(name.isNullOrEmpty() || email.isNullOrEmpty() || password.isNullOrEmpty() || this.binding.spnCity.selectedItemPosition==-1){
            this.binding.tvError.setText("Error: All fields must be filled in!")
            return@signUpUser
        }
        val city = this.binding.spnCity.selectedItem.toString()
        var role = "User"
        if (this.binding.cbOrganizer.isChecked)
            role = "Organizer"

        this.firebaseAuth
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this){task ->

                if (task.isSuccessful){
                    //create user document with default profile info
                    val newUser = User(email, password, name, city, role)
                    userRepository.addUserToDB(newUser)
                    Log.d(TAG, "signUpUser: User account successfully created with email $email")
                    Toast.makeText(this, "REGISTERED SUCCESSFULLY", Toast.LENGTH_SHORT).show()
                    finish()
                }else{
                    Log.d(TAG, "signUpUser: Unable to create user account : ${task.exception}", )
                    Toast.makeText(this, "COULD NOT REGISTER.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}