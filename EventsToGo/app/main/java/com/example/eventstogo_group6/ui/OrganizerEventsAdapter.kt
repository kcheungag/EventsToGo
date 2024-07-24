package com.example.eventstogo_group6.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.databinding.RowLayoutRvOrganizerEventsBinding

class OrganizerEventsAdapter(
    private val context: Context,
    private val itemList: ArrayList<Event>,
    private val clickListener: OnEventClickListener
) : RecyclerView.Adapter<OrganizerEventsAdapter.ItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            RowLayoutRvOrganizerEventsBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemList[position], clickListener)
    }

    class ItemViewHolder(var binding: RowLayoutRvOrganizerEventsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event, clickListener : OnEventClickListener) {
            binding.tvOEventTitle.text = "${event.name} (${event.eventID})"
            binding.tvOEventLocation.text = "${event.city}, ${event.country}"
            binding.tvOEventScheduleStart.text = "Start: ${event.scheduleStart}"
            binding.tvOEventScheduleEnd.text = "End: ${event.scheduleEnd}"
            binding.tvOEventSlotsLeft.text = "Slots Left: ${event.numberOfSlots}"
            binding.tvOEventPrice.text = if(event.price > 0) "$${event.price}" else "FREE"
            binding.tvOEventStatus.text = if (event.isAvailable) "OPEN" else "CLOSED"
            binding.tvOEventStatus.setTextColor(if(event.isAvailable) getColor(binding.root.context, R.color.green) else getColor(binding.root.context, R.color.red))

            itemView.setOnClickListener { clickListener.onEventSelected(event) }
        }
    }
}