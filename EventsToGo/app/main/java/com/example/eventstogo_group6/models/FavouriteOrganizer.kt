package com.example.eventstogo_group6.models

// FavoriteOrganizer data class
data class FavouriteOrganizer(
    val favoriteID: String = "",
    val organizerEmail: String = "",
    val userEmail: String = ""
) {
    constructor():this ("","","")
}
