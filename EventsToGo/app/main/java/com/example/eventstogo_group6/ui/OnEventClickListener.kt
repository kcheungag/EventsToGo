package com.example.eventstogo_group6.ui

import com.example.eventstogo_group6.models.Event

interface OnEventClickListener {
    fun onEventSelected(event: Event)
}