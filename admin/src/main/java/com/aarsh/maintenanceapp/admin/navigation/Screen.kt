package com.aarsh.maintenanceapp.admin.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val text: String) {
    object Dashboard : Screen(
        route = "dashboard",
        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
        text = "Dashboard"
    )
    object Requests : Screen(
        route = "requests",
        icon = { Icon(Icons.Default.List, contentDescription = "Requests") },
        text = "Requests"
    )
    object Announcements : Screen(
        route = "announcements",
        icon = { Icon(Icons.Default.Notifications, contentDescription = "Announcements") },
        text = "Announcements"
    )
    object Statistics : Screen(
        route = "statistics",
        icon = { Icon(Icons.Default.Info, contentDescription = "Statistics") },
        text = "Statistics"
    )
    object Profile : Screen(
        route = "profile",
        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
        text = "Profile"
    )
    object ComplaintsByStatus : Screen(
        route = "complaints_by_status/{status}",
        icon = { Icon(Icons.Default.List, contentDescription = "Complaints by Status") },
        text = "Complaints by Status"
    )
}

val adminTabRowScreens = listOf(
    Screen.Dashboard,
    Screen.Requests,
    Screen.Announcements,
    Screen.Statistics,
    Screen.Profile
) 