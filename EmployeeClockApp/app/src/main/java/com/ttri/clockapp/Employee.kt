package com.ttri.clockapp

// This is the Firestore document structure for each employee
data class Employee(
    val firstName: String = "",
    val lastName: String = "",
    val pin: String = ""   // must be 6 digits (string so we don’t lose leading zeros)
)
