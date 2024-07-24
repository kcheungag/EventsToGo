package com.example.eventstogo_group6.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.eventstogo_group6.models.Event

class EventViewModel : ViewModel() {
    private val _eventList = MutableLiveData<List<Event>>()
    val eventList: LiveData<List<Event>> get() = _eventList

    fun updateEventList(newList: List<Event>) {
        _eventList.value = newList
    }
}
