package com.example.eventstogo_group6.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.RegisteredEventsRepository
import com.example.eventstogo_group6.databinding.ActivityOrganizerEventsViewEventUsersBinding
import com.example.eventstogo_group6.enums.ExtrasRef
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.models.User
import com.example.eventstogo_group6.ui.OnEventUserClickListener
import com.example.eventstogo_group6.ui.OrganizerEventUsersAdapter
import com.example.eventstogo_group6.ui.OrganizerEventsAdapter

class OrganizerEventsViewEventUsersActivity : AppCompatActivity(), OnEventUserClickListener,
    OnClickListener {
    private val TAG = this.toString();
    private lateinit var binding: ActivityOrganizerEventsViewEventUsersBinding

    private lateinit var registeredEventsRepository: RegisteredEventsRepository

    private lateinit var adapter: OrganizerEventUsersAdapter
    private lateinit var usersList: ArrayList<User>

    private lateinit var currEvent: Event

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityOrganizerEventsViewEventUsersBinding.inflate(layoutInflater)
        setContentView(this.binding.root)

        this.binding.btnOMessageAll.setOnClickListener(this)

        usersList = ArrayList()
        adapter = OrganizerEventUsersAdapter(
            this, usersList, this
        )

        binding.rvOEventUser.adapter = adapter
        binding.rvOEventUser.layoutManager = LinearLayoutManager(this)
        binding.rvOEventUser.addItemDecoration(
            DividerItemDecoration(
                this, LinearLayoutManager.VERTICAL
            )
        )

        if (intent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currEvent = intent.getSerializableExtra(
                    ExtrasRef.CURRENT_EVENT.toString(), Event::class.java
                )!!
            } else {
                currEvent = intent.getSerializableExtra(ExtrasRef.CURRENT_EVENT.toString()) as Event
            }
        } else {
            finish()
        }

        registeredEventsRepository = RegisteredEventsRepository(applicationContext)
    }

    override fun onStart() {
        super.onStart()

        Log.d(TAG, "onStart: STARTING?")

        this.registeredEventsRepository.getRegisteredUsers(currEvent.eventID)
        this.registeredEventsRepository.eventRegisteredUsers.observe(this) { users ->
            if (users != null) {
                usersList.clear()
                usersList.addAll(users)
                usersList.sortBy({ it.name })
                adapter.notifyDataSetChanged()

                Log.d(TAG, "onStart: $usersList")
                binding.tvONoAttendees.visibility = if(usersList.isEmpty()) View.VISIBLE else View.GONE
                binding.btnOMessageAll.visibility = if(usersList.isEmpty()) View.GONE else View.VISIBLE
            } else {
                binding.tvONoAttendees.visibility = View.VISIBLE
                binding.btnOMessageAll.visibility = View.GONE
                Log.e(TAG, "onStart: Events empty")
            }
        }
    }

    override fun onRowSelected(input: String) {
        sendEmail(input)
    }

    override fun onClick(v: View?) {
        when (v!!) {
            binding.btnOMessageAll -> {
                var recipients = ""
                for (recipient in usersList) {
                    recipients += "${recipient.email},"
                }
                sendEmail(recipients)
            }
        }
    }

    fun sendEmail(recipients: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipients))
        intent.putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.app_name)}: ${currEvent.name} Event")
        startActivity(Intent.createChooser(intent,
            "Send Email Using: "))
    }
}