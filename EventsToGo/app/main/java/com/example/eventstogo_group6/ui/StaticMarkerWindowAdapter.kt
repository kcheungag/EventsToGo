package com.example.eventstogo_group6.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.models.Event
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class StaticWindowAdapter(
    private val context: Context,
    private val event: Event
) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker?): View? {
        return null // Return null to use the default info window
    }

    override fun getInfoContents(marker: Marker?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.static_marker_window_layout, null)

        // Find views in the layout
        val ivDirection: ImageView = view.findViewById(R.id.ivDirection)
        val tvEventBuilding: TextView = view.findViewById(R.id.tvEventBuilding)
        val tvEventAddress: TextView = view.findViewById(R.id.tvEventAddress)

        // Set data for the event
        tvEventBuilding.text = "${event.building}"
        tvEventAddress.text = "${event.street}, " +
                                "${event.city}, " +
                                "${event.country}"

        // Set up click listener for directions
        ivDirection.setOnClickListener {
            // Handle click, e.g., open Google Maps for directions
        }

        return view
    }
}
