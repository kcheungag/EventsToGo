package com.example.eventstogo_group6.models

import com.google.firebase.Timestamp
import java.io.Serializable
import java.util.Date
import java.util.UUID

data class Event(
    val eventID : String = UUID.randomUUID().toString(),
    val organizerEmail : String = "",
    var image : String = "",
    var name : String = "",
    var description : String = "",
    var street : String = "",
    var building : String = "",
    var city : String = "",
    var country : String = "",
    var scheduleStart : Date = Date(),
    var scheduleEnd : Date = Date(),
    var numberOfSlots : Int = 0,
    var price : Double = 0.0,
    @field:JvmField var isAvailable : Boolean = false,
    var advertising : String = "",
) : Serializable {
    val datePair: Pair<Date, Date>
        get() = Pair(scheduleStart, scheduleEnd)
}
