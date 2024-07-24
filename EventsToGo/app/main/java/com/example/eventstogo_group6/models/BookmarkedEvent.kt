package com.example.eventstogo_group6.models


// BookmarkedEvent data class
data class BookmarkedEvent(
    val bookmarkID: String="",
    val eventID: String="",
    val userEmail: String=""
){
    constructor(): this ("","","")
}
