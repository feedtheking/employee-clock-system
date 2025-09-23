package com.ttri.clockapp

// Matches what we’ll read on the kiosk side.
// Defaults keep old docs working (missing employedStatus => treated as active).
data class Employee(
    val employeeID: String = "",      // unique code/slug for this employee
    val firstName: String = "",
    val lastName: String = "",
    val pin: String = "",             // 6 digits as string (keeps leading zeros)
    val store: String = "",           // e.g., "StoreA"
    val employedStatus: Boolean = true
)
