package com.example.eventstogo_group6.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.EventRepository
import com.example.eventstogo_group6.databinding.ActivityOrganizerEventsBinding
import com.example.eventstogo_group6.enums.ExtrasRef
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.ui.BaseUI
import com.example.eventstogo_group6.ui.OnEventClickListener
import com.example.eventstogo_group6.ui.OrganizerEventsAdapter

class OrganizerEventsActivity : BaseUI(), OnClickListener, OnEventClickListener {
    private val TAG = this.toString();
    private lateinit var binding: ActivityOrganizerEventsBinding

    private lateinit var eventRepository: EventRepository

    private lateinit var adapter: OrganizerEventsAdapter
    private lateinit var eventsList: ArrayList<Event>
    private lateinit var allEventsList: ArrayList<Event>

    private var filterOpen = true
    private var filterClosed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityOrganizerEventsBinding.inflate(layoutInflater)
        this.binding = ActivityOrganizerEventsBinding.inflate(layoutInflater)
        setContentView(this.binding.root)

//        setSupportActionBar(this.binding.menuToolbar)

        binding.btnOAddEvent.setOnClickListener(this)
        binding.btnOFilterOpen.setOnClickListener(this)
        binding.btnOFilterClosed.setOnClickListener(this)
        binding.ibtnOSearch.setOnClickListener(this)

        allEventsList = ArrayList()
        eventsList = ArrayList()
        adapter = OrganizerEventsAdapter(
            this, eventsList, this
        )

        binding.rvOEvents.adapter = adapter
        binding.rvOEvents.layoutManager = LinearLayoutManager(this)
        binding.rvOEvents.addItemDecoration(
            DividerItemDecoration(
                this, LinearLayoutManager.VERTICAL
            )
        )

        eventRepository = EventRepository(applicationContext)
    }

    override fun onStart() {
        super.onStart()

        this.eventRepository.getOrganizerEvents(userEmail)
        this.eventRepository.allEvents.observe(this) { events ->
            if (events != null) {
                allEventsList.clear()
                allEventsList.addAll(events)

                updateEventsList()
            } else {
                binding.tvONoEvents.visibility = View.VISIBLE
                Log.e(TAG, "onStart: Events empty")
            }
        }
    }

    private fun updateEventsList() {
        val filteredList = arrayListOf<Event>()
        filteredList.addAll(allEventsList)
        val eFilterQuery = binding.etOEFilter.text.toString().trim().lowercase()

        if(!filterOpen) filteredList.removeIf { it.isAvailable  }
        if(!filterClosed) filteredList.removeIf { !it.isAvailable }
        if(!eFilterQuery.isEmpty()) filteredList.removeIf { !it.name.lowercase().contains(eFilterQuery) && !it.eventID.lowercase().contains(eFilterQuery) }

        val openCount = filteredList.count { it.isAvailable }
        val closedCount = filteredList.count{ !it.isAvailable }

        binding.btnOFilterOpen.text = "OPEN ($openCount)"
        binding.btnOFilterClosed.text = "CLOSED ($closedCount)"

        eventsList.clear()
        eventsList.addAll(filteredList)
        eventsList
            .sortWith(compareByDescending<Event> { it.isAvailable }
                .thenBy { it.scheduleStart }
                .thenBy { it.name })

        adapter.notifyDataSetChanged()
        binding.tvONoEvents.visibility = if(eventsList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onClick(v: View?) {
        when (v!!) {
            binding.btnOAddEvent -> {
                val intent = Intent(this, OrganizerModifyEventsActivity::class.java)
                intent.putExtra(ExtrasRef.IS_EVENT_UPDATE.toString(), false)
                intent.putExtra(ExtrasRef.CURRENT_USER.toString(), userEmail)
                startActivity(intent)
            }
            binding.btnOFilterOpen -> {
                filterOpen = !filterOpen
                binding.btnOFilterOpen.setBackgroundColor(
                    if(filterOpen) getColor(R.color.green)
                    else getColor(R.color.subtitle)
                )
                updateEventsList()
            }
            binding.btnOFilterClosed -> {
                filterClosed = !filterClosed
                binding.btnOFilterClosed.setBackgroundColor(
                    if(filterClosed) getColor(R.color.red)
                    else getColor(R.color.subtitle)
                )
                updateEventsList()
            }
            binding.ibtnOSearch -> {
                updateEventsList()
            }
        }
    }

    override fun onEventSelected(event: Event) {
        val intent = Intent(this, OrganizerEventsViewDetailsActivity::class.java)
        intent.putExtra(ExtrasRef.IS_EVENT_UPDATE.toString(), true)
        intent.putExtra(ExtrasRef.CURRENT_EVENT.toString(), event)
        startActivity(intent)
    }
}