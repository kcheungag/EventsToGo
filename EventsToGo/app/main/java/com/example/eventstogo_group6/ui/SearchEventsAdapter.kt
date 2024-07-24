package com.example.eventstogo_group6.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.databinding.EventItemLayoutBinding
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.screens.EventDetails
import com.google.android.gms.maps.model.LatLng
import com.squareup.picasso.Picasso

class SearchEventsAdapter(
    private val events: List<Event>,
    private var originalList: MutableList<Event> = events.toMutableList(),
    private var filteredList: MutableList<Event> = originalList.toMutableList(),
    private var userLocation: LatLng? = null,
    private var searchRadiusKm: Float = 50f,
    private var sortingOption: String? = null,
    private val shareIconHandler: (Int) -> Unit,
    private val starIconHandler: (Int) -> Unit,
    ) : RecyclerView.Adapter<SearchEventsAdapter.ItemViewHolder>() {

    val itemList: List<Event>
        get() = events

    inner class ItemViewHolder(val binding: EventItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val eventID = filteredList[adapterPosition].eventID
                val intent = Intent(binding.root.context, EventDetails::class.java)
                intent.putExtra("eventID", eventID)
                binding.root.context.startActivity(intent)
            }

            binding.ivStar.setOnClickListener {
                // Favourite Event Logic
                // it will save event to user's
                // it will show snackbar when user is not loggedin
                starIconHandler(adapterPosition)

            }

            binding.ivShare.setOnClickListener {
                // Share Logic
                // it should display a dialogue contains the options of sharing
                // when clicked a msg contains the event details would be prepared
                shareIconHandler(adapterPosition)
                //val event = filteredList[adapterPosition]
                //shareClickListener?.onShareClick(event)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            EventItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currItem = filteredList[position]

        // You can update the views in the layout with data from the Event object
        Picasso.with(holder.itemView.context).load(currItem.image).placeholder(holder.itemView.context.getDrawable(R.drawable.ic_launcher_background)).into(holder.binding.eventImage)
        holder.binding.eventTitle.text = currItem.name
        val name = EmailUtils.extractNameFromEmail(currItem.organizerEmail)
        holder.binding.eventOrganiser.text = "Organized by: $name"
        holder.binding.price.text =
            if (currItem.price != 0.0) "$${currItem.price}" else "Free"

        // Example: Set event image (Assuming you have an ImageView with id 'eventImage')
        // holder.binding.eventImage.setImageResource(currItem.image)
    }

    fun filter(userLocation: LatLng, searchRadiusKm: Float, sortingOption: String) {
        this.userLocation = userLocation
        this.searchRadiusKm = searchRadiusKm
        this.sortingOption = sortingOption
        // Implement your filtering logic based on geolocation and sortingOption
        // Update itemList accordingly
        // notifyDataSetChanged() to refresh the RecyclerView
        notifyDataSetChanged()
    }

    fun secondSearch(query: String) {
        filteredList = originalList.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.city.contains(query, ignoreCase = true) ||
                    it.country.contains(query, ignoreCase = true)
        }.toMutableList()
        notifyDataSetChanged()
    }

    fun updateList(newList: List<Event>) {
        originalList.clear()
        originalList.addAll(newList)
        userLocation?.let { filter(it, searchRadiusKm, sortingOption ?: "DefaultSortingOption") }
    }
}

