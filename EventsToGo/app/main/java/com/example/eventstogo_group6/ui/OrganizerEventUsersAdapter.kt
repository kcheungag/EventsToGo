package com.example.eventstogo_group6.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eventstogo_group6.databinding.RowLayoutRvOrganizerEventUsersTextBinding
import com.example.eventstogo_group6.models.User

class OrganizerEventUsersAdapter(
    private val context: Context,
    private val itemList: ArrayList<User>,
    private val clickListener: OnEventUserClickListener
) : RecyclerView.Adapter<OrganizerEventUsersAdapter.ItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            RowLayoutRvOrganizerEventUsersTextBinding.inflate(
                LayoutInflater.from(context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemList[position], clickListener)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    class ItemViewHolder(var binding: RowLayoutRvOrganizerEventUsersTextBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User, clickListener: OnEventUserClickListener) {
            binding.tvTitle.text = user.name
            binding.tvDescription.text = user.email
            itemView.setOnClickListener { clickListener.onRowSelected(user.email) }
        }
    }
}