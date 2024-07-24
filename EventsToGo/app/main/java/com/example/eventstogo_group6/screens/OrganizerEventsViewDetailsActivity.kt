package com.example.eventstogo_group6.screens

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.EventRepository
import com.example.eventstogo_group6.databinding.ActivityOrganizerEventsViewDetailsBinding
import com.example.eventstogo_group6.enums.ExtrasRef
import com.example.eventstogo_group6.models.Event
import com.squareup.picasso.Picasso


class OrganizerEventsViewDetailsActivity : AppCompatActivity(), OnClickListener {
    private val TAG = this.toString()
    private lateinit var binding: ActivityOrganizerEventsViewDetailsBinding

    private lateinit var eventRepository: EventRepository

    private lateinit var currEvent: Event
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityOrganizerEventsViewDetailsBinding.inflate(layoutInflater)
        setContentView(this.binding.root)

        this.binding.btnEViewAttendees.setOnClickListener(this)
        this.binding.btnEUpdateEvent.setOnClickListener(this)
        this.binding.btnEDeleteEvent.setOnClickListener(this)

        if (intent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                currEvent = intent.getSerializableExtra(
                    ExtrasRef.CURRENT_EVENT.toString(), Event::class.java
                )!!
            } else {
                currEvent = intent.getSerializableExtra(ExtrasRef.CURRENT_EVENT.toString()) as Event
            }
            refreshUI()
        } else {
            finish()
        }

        eventRepository = EventRepository(applicationContext)
    }

    override fun onStart() {
        super.onStart()

        this.eventRepository.getEventUpdatedImage(currEvent.eventID)
        this.eventRepository.updatedImageURL.observe(this) { downloadURL ->
            if (downloadURL.isNullOrEmpty()) {
                Log.e(TAG, "onStart: no download URL")
            } else {
                currEvent.image = downloadURL
                try {
                    Picasso.with(this).load(currEvent.image)
                        .placeholder(getDrawable(R.drawable.ic_launcher_background))
                        .into(binding.imgEVDImage)
                } catch (ex: Exception) {
                    Log.e(TAG, "refreshUI: $ex")
                    binding.imgEVDImage.setImageDrawable(getDrawable(R.drawable.ic_launcher_background))
                }
            }
        }
    }

    private fun refreshUI() {
        this.binding.tvEName.text = currEvent.name
        this.binding.tvEAvailability.text = if (currEvent.isAvailable) "OPEN" else "CLOSED"
        this.binding.tvEAvailability.setTextColor(
            if (currEvent.isAvailable) ContextCompat.getColor(
                binding.root.context, R.color.green
            ) else ContextCompat.getColor(binding.root.context, R.color.red)
        )
        this.binding.tvEID.text = currEvent.eventID
        this.binding.tvEAdDisplay.text = currEvent.advertising
        this.binding.tvEDescription.text = currEvent.description
        this.binding.tvEStartSchedule.text = "Start: ${currEvent.scheduleStart}"
        this.binding.tvEEndSchedule.text = "End: ${currEvent.scheduleEnd}"
        this.binding.tvEPrice.text = "Price: $${currEvent.price}"
        this.binding.tvESlots.text = "Number of Slots: ${currEvent.numberOfSlots}"
        this.binding.tvEAddress.text =
            "${currEvent.building}, ${currEvent.street}, ${currEvent.city}, ${currEvent.country}"
        this.binding.tvEImage.text = currEvent.image
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val intent: Intent = result.data!!
                    if (intent == null) finish()
                    else {
                        val updatedEvent =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getSerializableExtra(
                                    ExtrasRef.CURRENT_EVENT.toString(), Event::class.java
                                )
                            } else {
                                intent.getSerializableExtra(ExtrasRef.CURRENT_EVENT.toString()) as Event
                            }
                        currEvent = updatedEvent!!
                        refreshUI()
                    }
                }
            }
        }

    override fun onClick(v: View?) {
        when (v!!) {
            this.binding.btnEViewAttendees -> {
                val intent = Intent(this, OrganizerEventsViewEventUsersActivity::class.java)
                intent.putExtra(ExtrasRef.CURRENT_EVENT.toString(), currEvent)
                startActivity(intent)
            }

            this.binding.btnEUpdateEvent -> {
                val intent = Intent(this, OrganizerModifyEventsActivity::class.java)
                intent.putExtra(ExtrasRef.IS_EVENT_UPDATE.toString(), true)
                intent.putExtra(ExtrasRef.CURRENT_EVENT.toString(), currEvent)
                intent.putExtra(ExtrasRef.CURRENT_USER.toString(), currEvent.organizerEmail)
                startForResult.launch(intent)
            }

            this.binding.btnEDeleteEvent -> {
                val builder = AlertDialog.Builder(this@OrganizerEventsViewDetailsActivity)
                builder.setMessage("Are you sure you want to delete this event?")
                    .setCancelable(false).setNegativeButton("Yes") { dialog, id ->
                        eventRepository.delEvent(currEvent)
                        finish()
                    }.setPositiveButton("NO") { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.getButton(DialogInterface.BUTTON_POSITIVE)
                    ?.setTextColor(getColor(R.color.red))
                alert.getButton(DialogInterface.BUTTON_NEGATIVE)
                    ?.setTextColor(getColor(R.color.green))
                alert.show()
            }
        }
    }
}