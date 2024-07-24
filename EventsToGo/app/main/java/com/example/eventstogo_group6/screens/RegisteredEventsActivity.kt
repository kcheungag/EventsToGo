package com.example.eventstogo_group6.screens

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.EventRepository
import com.example.eventstogo_group6.database.RegisteredEventsRepository
import com.example.eventstogo_group6.databinding.ActivityMainBinding
import com.example.eventstogo_group6.databinding.ActivityRegisteredEventsBinding
import com.example.eventstogo_group6.enums.SharedPrefRef
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.models.Registration
import com.example.eventstogo_group6.ui.OrganizerEventsAdapter
import com.example.eventstogo_group6.ui.RegisteredEventsAdapter
import com.google.firebase.firestore.FirebaseFirestore

class RegisteredEventsActivity : AppCompatActivity() {

    private val TAG: String = this@RegisteredEventsActivity.toString()
    private lateinit var binding: ActivityRegisteredEventsBinding
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var registeredEventsRepository: RegisteredEventsRepository
    private lateinit var eventRepository: EventRepository
    private val firestore = FirebaseFirestore.getInstance()


    private lateinit var registrationsList : ArrayList<Registration>
    private lateinit var eventIDList : ArrayList<String>
    private lateinit var eventsList: ArrayList<Event>

    private lateinit var adapter: RegisteredEventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisteredEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(SharedPrefRef.SHARED_PREF_NAME.value, MODE_PRIVATE)

        registeredEventsRepository = RegisteredEventsRepository(applicationContext)
        eventRepository = EventRepository(applicationContext)

        registrationsList = ArrayList()
        eventIDList = ArrayList()
        eventsList = ArrayList()

        adapter = RegisteredEventsAdapter(
            eventsList,
            registrationsList,
            rowClickHandler = { pos -> eventRowClicked(pos) },
            cancelBtnClickHandler = { pos -> cancelButtonClicked(pos) }
        )
        binding.rvRegEvents.adapter = adapter
        binding.rvRegEvents.layoutManager = LinearLayoutManager(this)
        binding.rvRegEvents.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
    }

    private fun eventRowClicked(pos: Int){
        val intent = Intent(this, RegisteredEventDetails::class.java)
        intent.putExtra("eventID", eventsList.get(pos).eventID)
        startActivity(intent)
    }

    private fun cancelButtonClicked(pos: Int) {
        var regID = ""
        var regCount = 0
        for(reg in registrationsList)
        {
            if(reg.eventID==eventsList.get(pos).eventID) {
                regID = reg.registrationID
                regCount = reg.quantity
                break
            }
        }
        updateNumberOfSlots(eventsList.get(pos).eventID, regCount)
        this.registeredEventsRepository.deleteRegistration(regID)
        finish()
        startActivity(Intent(this, RegisteredEventsActivity::class.java))
    }

    private fun updateNumberOfSlots(eventID: String, quantity: Int) {
        // Retrieve the event document from Firestore
        firestore.collection("Events")
            .document(eventID)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Document exists, update the numberOfSlots
                    val event = documentSnapshot.toObject(Event::class.java)

                    // Update the numberOfSlots locally
                    event?.let {
                        it.numberOfSlots += quantity
                        it.isAvailable=true
                        // Update the numberOfSlots in Firestore
                        firestore.collection("Events")
                            .document(eventID)
                            .set(it)
                            .addOnSuccessListener {
                                // Successfully updated numberOfSlots in Firestore
                                Log.d(TAG, "Cancelled ticket count added back to event")
                            }
                            .addOnFailureListener { e ->
                                // Handle the failure case
                                Log.d(TAG, "Cancelled ticket count not added back to event")

                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle the failure case
                Log.d(TAG, "Order Added - $e")

            }
    }

    override fun onStart() {
        super.onStart()

        val currentUser: String? = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")
        if(!currentUser.isNullOrEmpty()){
            this.registeredEventsRepository.getRegistrations(currentUser)
            this.registeredEventsRepository.registrations.observe(this) { registrations ->
                if(registrations != null) {
                    registrationsList.clear()
                    registrationsList.addAll(registrations)
                    for(reg in registrationsList)
                        eventIDList.add(reg.eventID)
                    Log.d(TAG, "onStart: $eventIDList")
                    this.eventRepository.getRegisteredEvents(eventIDList)
                    this.eventRepository.allEvents.observe(this) { events ->
                        if (events != null) {
                            eventsList.clear()
                            eventsList.addAll(events)
                            Log.d(TAG, "onStart: $eventsList")
                            adapter.notifyDataSetChanged()
                        } else {
                            Log.e(TAG, "onStart: Events empty")
                        }
                    }
                    binding.tvNoEvents.visibility = if(registrationsList.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    binding.tvNoEvents.visibility = View.VISIBLE
                    Log.e(TAG, "onStart: Events empty")
                }
            }
        }

    }
}