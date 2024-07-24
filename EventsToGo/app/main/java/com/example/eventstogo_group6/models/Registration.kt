package com.example.eventstogo_group6.models

import java.util.UUID

class Registration (
    val registrationID : String = UUID.randomUUID().toString(),
    val eventID : String = "",
    var userEmail : String = "",
    val quantity: Int = 0,
    val shippingAddress: String = "",
)