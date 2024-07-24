package com.example.eventstogo_group6.screens

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.EventRepository
import com.example.eventstogo_group6.database.FavouriteEventsRepository
import com.example.eventstogo_group6.database.FavouriteOrganizerRepository
import com.example.eventstogo_group6.database.RegisteredEventsRepository
import com.example.eventstogo_group6.database.UserRepository
import com.example.eventstogo_group6.databinding.ActivityFavouriteEventsBinding
import com.example.eventstogo_group6.databinding.ActivityRegisteredEventsBinding
import com.example.eventstogo_group6.enums.SharedPrefRef
import com.example.eventstogo_group6.models.BookmarkedEvent
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.models.FavouriteOrganizer
import com.example.eventstogo_group6.models.Registration
import com.example.eventstogo_group6.models.User
import com.example.eventstogo_group6.ui.FavoriteOrganizerAdapter
import com.example.eventstogo_group6.ui.FavouriteEventAdapter
import com.example.eventstogo_group6.ui.RegisteredEventsAdapter
import com.google.firebase.firestore.FirebaseFirestore

class FavouriteEvents : AppCompatActivity() {

    private val TAG: String = this@FavouriteEvents.toString()
    private lateinit var binding: ActivityFavouriteEventsBinding
    lateinit var sharedPreferences: SharedPreferences
    private lateinit var favouriteEventsRepository: FavouriteEventsRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var favouriteOrganizerRepository: FavouriteOrganizerRepository
    private lateinit var userRepository: UserRepository
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var favouriteEventList: ArrayList<BookmarkedEvent>
    private lateinit var eventIDList: ArrayList<String>
    private lateinit var eventsList: ArrayList<Event>
    private lateinit var favouriteOrganizerList: ArrayList<FavouriteOrganizer>
    private lateinit var userIDList: ArrayList<String>
    private lateinit var userList: ArrayList<User>

    private lateinit var adapter1: FavouriteEventAdapter
    private lateinit var adapter2: FavoriteOrganizerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouriteEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(SharedPrefRef.SHARED_PREF_NAME.value, MODE_PRIVATE)

        favouriteEventsRepository = FavouriteEventsRepository(applicationContext)
        eventRepository = EventRepository(applicationContext)
        favouriteOrganizerRepository = FavouriteOrganizerRepository(applicationContext)
        userRepository = UserRepository(applicationContext)

        favouriteEventList = ArrayList()
        eventIDList = ArrayList()
        eventsList = ArrayList()
        favouriteOrganizerList = ArrayList()
        userIDList = ArrayList()
        userList = ArrayList()

        adapter1 = FavouriteEventAdapter(
            eventsList,
            rowClickHandler = { pos -> eventRowClicked(pos) },
            cancelBtnClickHandler = { pos -> cancelButtonClicked(pos) }
        )
        binding.rvFavEvents.adapter = adapter1
        binding.rvFavEvents.layoutManager = LinearLayoutManager(this)
        binding.rvFavEvents.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
        adapter2 = FavoriteOrganizerAdapter(
            userList,
            { pos -> removeButtonClicked(pos) }
        )
        binding.rvFavOrganizers.adapter = adapter2
        binding.rvFavOrganizers.layoutManager = LinearLayoutManager(this)
        binding.rvFavOrganizers.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
    }

    private fun removeButtonClicked(pos: Int) {
        var favID = ""
        for (fav in favouriteOrganizerList) {
            if (fav.organizerEmail == userList.get(pos).email) {
                favID = fav.favoriteID
                break
            }
        }
        Log.d(TAG, "removeButtonClicked: Fav ID: $favID")
        this.favouriteOrganizerRepository.deleteFavOrganizer(favID)
        finish()
        startActivity(Intent(this, FavouriteEvents::class.java))
    }

    private fun eventRowClicked(pos: Int) {
        val intent = Intent(this, EventDetails::class.java)
        intent.putExtra("eventID", eventsList.get(pos).eventID)
        startActivity(intent)
    }

    private fun cancelButtonClicked(pos: Int) {
        var bookmarkID = ""
        for (item in favouriteEventList) {
            if (item.eventID == eventsList.get(pos).eventID) {
                bookmarkID = item.bookmarkID
                break
            }
        }
        this.favouriteEventsRepository.deleteFavouriteEvent(bookmarkID)
        finish()
        startActivity(Intent(this, FavouriteEvents::class.java))
    }

    override fun onStart() {
        super.onStart()

        val currentUser: String? = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")
        if (currentUser != "") {
            this.favouriteEventsRepository.getFavouriteEvents(currentUser.toString())
                .observe(this) { favEvent ->
                    if (favEvent != null) {
                        favouriteEventList.clear()
                        favouriteEventList.addAll(favEvent)
                        for (fav in favouriteEventList)
                            eventIDList.add(fav.eventID)
                        Log.d(TAG, "onStart: $eventIDList")
                        this.eventRepository.getRegisteredEvents(eventIDList)
                        this.eventRepository.allEvents.observe(this) { events ->
                            if (events != null) {
                                eventsList.clear()
                                eventsList.addAll(events)
                                Log.d(TAG, "onStart: $eventsList")
                                adapter1.notifyDataSetChanged()
                            } else {
                                Log.e(TAG, "onStart: Events empty")
                            }
                        }
                        binding.tvNoFavEvents.visibility =
                            if (favouriteEventList.isEmpty()) View.VISIBLE else View.GONE
                    } else {
                        binding.tvNoFavEvents.visibility = View.VISIBLE
                        Log.e(TAG, "onStart: Events empty")
                    }
                }
            this.favouriteOrganizerRepository.getFavouriteOrganizers(currentUser.toString())
                .observe(this) { favOrg ->
                    if (favOrg != null) {
                        favouriteOrganizerList.clear()
                        favouriteOrganizerList.addAll(favOrg)
                        for (fav in favouriteOrganizerList)
                            userIDList.add(fav.organizerEmail)
                        Log.d(TAG, "onStart: $userIDList")
                        this.userRepository.getUsersByID(userIDList).observe(this) { users ->
                            if (users != null) {
                                userList.clear()
                                userList.addAll(users)
                                Log.d(TAG, "onStart: $userList")
                                adapter2.notifyDataSetChanged()
                            } else {
                                Log.e(TAG, "onStart: Events empty")
                            }
                        }
                        binding.tvNoFavOrg.visibility =
                            if (favouriteOrganizerList.isEmpty()) View.VISIBLE else View.GONE
                    }
                    else {
                        binding.tvNoFavOrg.visibility = View.VISIBLE
                        Log.e(TAG, "onStart: Events empty")
                    }
                }

        }

    }
}