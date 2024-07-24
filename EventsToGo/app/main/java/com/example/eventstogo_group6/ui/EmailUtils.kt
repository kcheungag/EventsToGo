package com.example.eventstogo_group6.ui

class EmailUtils {
    companion object {
        fun extractNameFromEmail(email: String): String {
            val atIndex = email.indexOf('@')
            if (atIndex != -1) {
                val username = email.substring(0, atIndex)
                // Capitalize the first letter and convert the rest to lowercase
                return username.capitalize()
            }
            return email // return the original email if '@' is not found
        }

        fun createEmailFromName(name: String): String {
            // Convert the name to lowercase
            val lowerCaseName = name.toLowerCase()

            // Replace spaces with underscores and concatenate with a domain (e.g., example.com)
            return "$lowerCaseName@gmail.com"
        }

    }
}