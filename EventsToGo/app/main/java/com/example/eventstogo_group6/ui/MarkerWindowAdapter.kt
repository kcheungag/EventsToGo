package com.example.eventstogo_group6.ui

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.screens.EventDetails
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.squareup.picasso.Picasso
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date


class MarkerWindowAdapter(
    private val context: Context,
    private val eventList: MutableList<Event>
) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker?): View? {
        // Return null to use the default info window frame
        return null
    }

    override fun getInfoContents(marker: Marker?): View {
        val eventId = marker?.snippet
        val event = eventList.find { it.eventID == eventId }

        // Inflate the custom info window layout
        val view = LayoutInflater.from(context).inflate(R.layout.marker_window_layout, null)

        // Get the views from the layout
        val eventImage = view.findViewById<ImageView>(R.id.ivEvent)
        val eventTitle = view.findViewById<TextView>(R.id.tvEventTitle)
        val eventDateTimeStart = view.findViewById<TextView>(R.id.tvEventDateStart)
        val eventDateTimeEnd = view.findViewById<TextView>(R.id.tvEventDateEnd)
        //val btnOpenEventDetails = view.findViewById<Button>(R.id.btnOpenEventDetails)

        // Customize the views based on event data
        if (event != null) {
            Picasso.with(context)
                .load(event.image)
                .placeholder(R.drawable.ic_launcher_background) // Placeholder image while loading
                .into(eventImage)

            eventTitle.text = event.name

            // Convert Timestamp to Date and then format it
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startDate = dateFormat.format(event.scheduleStart)
            val endDate = dateFormat.format(event.scheduleEnd)

            eventDateTimeStart.text = "Starting From: $startDate"
            eventDateTimeEnd.text = "Ending To: $endDate"


            // Set the event ID as a tag for the button
            //btnOpenEventDetails.tag = event.eventID
        }

        /*btnOpenEventDetails.setOnClickListener {
            // Extract event ID from the button's tag
            val eventId = it.tag as? String

            // Open EventDetails activity with the event ID
            val intent = Intent(context, EventDetails::class.java)
            intent.putExtra("eventId", eventId)
            context.startActivity(intent)
        }*/

        return view
    }

    fun openEventDetails(view: View){
            // Extract event ID from the marker or use any other logic to get the ID
            val eventId = view.tag as? String

            // Open EventDetails activity with the event ID
            val intent = Intent(context, EventDetails::class.java)
            intent.putExtra("eventId", eventId)
            context.startActivity(intent)
        }

    }

