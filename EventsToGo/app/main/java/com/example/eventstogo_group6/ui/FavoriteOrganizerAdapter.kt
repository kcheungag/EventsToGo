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
import com.example.eventstogo_group6.models.FavouriteOrganizer
import com.example.eventstogo_group6.models.User
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class FavoriteOrganizerAdapter(
    private val organizerList:MutableList<User>,
    private val removeBtnClickHandler: (Int) -> Unit) : RecyclerView.Adapter<FavoriteOrganizerAdapter.FavoriteOrganizerViewHolder>() {

    inner class FavoriteOrganizerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.findViewById<Button>(R.id.btn_remove).setOnClickListener {
                removeBtnClickHandler(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteOrganizerViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_layout_rv_fav_organizers, parent, false)
        return FavoriteOrganizerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return organizerList.size
    }

    override fun onBindViewHolder(holder: FavoriteOrganizerViewHolder, position: Int) {
        val currOrganizer: User = organizerList.get(position)

        val tvName = holder.itemView.findViewById<TextView>(R.id.tvName)
        tvName.text = currOrganizer.name
        val tvEmail = holder.itemView.findViewById<TextView>(R.id.tvEmail)
        tvEmail.text = currOrganizer.email

    }
}