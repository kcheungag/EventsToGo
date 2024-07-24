package com.example.eventstogo_group6.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.eventstogo_group6.MainActivity
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.UserRepository
import com.example.eventstogo_group6.enums.SharedPrefRef
import com.example.eventstogo_group6.models.User
import com.example.eventstogo_group6.screens.FavouriteEvents
import com.example.eventstogo_group6.screens.LoginActivity
import com.example.eventstogo_group6.screens.OrganizerEventsActivity
import com.example.eventstogo_group6.screens.RegisteredEventsActivity
import com.example.eventstogo_group6.screens.UserAccountActivity


open class BaseUI : AppCompatActivity() {
    private val TAG = this.toString()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var prefEditor: SharedPreferences.Editor

    private lateinit var userRepository: UserRepository
    lateinit var userEmail: String
    var fetchedUser: User? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.sharedPreferences =
            getSharedPreferences(SharedPrefRef.SHARED_PREF_NAME.value, MODE_PRIVATE)
        this.prefEditor = this.sharedPreferences.edit()

        userRepository = UserRepository(applicationContext)

        this.userRepository.singleUser.observe(this) {
            Log.d(TAG, "singleUser observe: $it")
            invalidateOptionsMenu()
            fetchedUser = it
        }
    }

    override fun onStart() {
        super.onStart()
        userEmail =
            this.sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "").toString()
        Log.d(TAG, "onStart: $userEmail")
    }

    override fun onResume() {
        super.onResume()

        userEmail =
            this.sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "").toString()
        Log.d(TAG, "onResume: $userEmail")
        this.userRepository.getUserFromDB(userEmail)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(!userEmail.isEmpty()) {
            menuInflater.inflate(
                R.menu.options_menu_items, menu
            ) // menu_options is the Android Resource File name


            menu!!.findItem(R.id.miOEvents).isVisible = fetchedUser?.role == "Organizer"
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mi_favorites -> {
                val intent = Intent(this, FavouriteEvents::class.java)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                startActivity(intent)
                return true
            }

            R.id.mi_logout -> {
                prefEditor.putString(SharedPrefRef.CURRENT_USER.value, "").apply()
                userRepository.getUserFromDB("")
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                startActivity(intent)
                return true
            }

            R.id.miOEvents -> {
                if (fetchedUser == null) {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                    startActivity(intent)
                    return false
                }

                val intent = Intent(this, OrganizerEventsActivity::class.java)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                startActivity(intent)
                return true
            }

            R.id.mi_reg_events -> {
                val intent = Intent(this, RegisteredEventsActivity::class.java)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                startActivity(intent)
                return true
            }

            R.id.mi_user_account -> {
                val intent = Intent(this, UserAccountActivity::class.java)
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                startActivity(intent)
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}