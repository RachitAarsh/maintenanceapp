package com.aarsh.maintenanceapp.data

import java.util.Date

data class Complaint(
    val id: String = "",
    val name: String = "",
    val designation: String = "",
    val complaint: String = "",
    val date: Date = Date(),
    val status: String = "Pending" // Can be: Pending, In Progress, Resolved
) 