package com.example.eventstogo_group6.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.models.Event
import com.squareup.picasso.Picasso
import java.security.AccessController.getContext
import java.text.SimpleDateFormat
import java.util.Locale

class FavouriteEventAdapter (
    private val eventList:MutableList<Event>,
    private val rowClickHandler: (Int) -> Unit,
    private val cancelBtnClickHandler: (Int) -> Unit) : RecyclerView.Adapter<FavouriteEventAdapter.RegisteredEventsViewHolder>() {

    inner class RegisteredEventsViewHolder(itemView: View) : RecyclerView.ViewHolder (itemView) {
        init {
            itemView.setOnClickListener {
                rowClickHandler(adapterPosition)
            }
            itemView.findViewById<Button>(R.id.btnCancelRegistration).setOnClickListener {
                cancelBtnClickHandler(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegisteredEventsViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.row_layout_rv_registered_events, parent, false)
        return RegisteredEventsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return eventList.size
    }

    override fun onBindViewHolder(holder: RegisteredEventsViewHolder, position: Int) {
        val currEvent:Event = eventList.get(position)

        val tvTitle = holder.itemView.findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = currEvent.name

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val tvDate = holder.itemView.findViewById<TextView>(R.id.tvDate)
        tvDate.text = "Date: ${dateFormat.format(currEvent.scheduleStart)}"

        val tvLocation = holder.itemView.findViewById<TextView>(R.id.tvLocation)
        tvLocation.text = "Location: ${currEvent.street}, ${currEvent.city}, ${currEvent.country}"

        Picasso.with(holder.itemView.context).load(currEvent.image).into(holder.itemView.findViewById<ImageView>(R.id.event_pic))
    }
}
