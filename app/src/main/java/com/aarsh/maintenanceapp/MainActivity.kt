package com.aarsh.maintenanceapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aarsh.maintenanceapp.ui.AnnouncementsScreen
import com.aarsh.maintenanceapp.ui.NewComplaintScreen
import com.aarsh.maintenanceapp.ui.StatusScreen
import com.aarsh.maintenanceapp.ui.SplashScreen
import com.aarsh.maintenanceapp.ui.AuthScreen
import com.aarsh.maintenanceapp.ui.ProfileScreen
import com.aarsh.maintenanceapp.ui.theme.MaintenanceAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val text: String) {
    object Splash : Screen(
        route = "splash",
        icon = { Icon(Icons.Filled.List, contentDescription = "Splash") },
        text = "Splash"
    )
    object Status : Screen(
        route = "status",
        icon = { Icon(Icons.Filled.List, contentDescription = "Status") },
        text = "Status"
    )
    object NewComplaint : Screen(
        route = "new_complaint",
        icon = { Icon(Icons.Filled.Add, contentDescription = "New Complaint") },
        text = "New"
    )
    object Announcements : Screen(
        route = "announcements",
        icon = { Icon(Icons.Filled.Info, contentDescription = "Announcements") },
        text = "Announcements"
    )
    object Profile : Screen(
        route = "profile",
        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
        text = "Profile"
    )
}

private val maintenanceTabRowScreens = listOf(
    Screen.Status,
    Screen.NewComplaint,
    Screen.Announcements,
    Screen.Profile
)

@Composable
fun MaintenanceTabRow(
    allScreens: List<Screen>,
    onTabSelected: (Screen) -> Unit,
    currentScreen: Screen
) {
    NavigationBar {
        allScreens.forEach { screen ->
            val selected = currentScreen == screen
            NavigationBarItem(
                icon = {
                    screen.iconWithColor(Color.Black)()
                },
                label = {
                    Text(
                        screen.text,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 0.dp)
                    )
                },
                selected = selected,
                onClick = { onTabSelected(screen) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

fun Screen.iconWithColor(color: Color): @Composable () -> Unit = when (this) {
    is Screen.Splash -> { { Icon(Icons.Filled.List, contentDescription = "Splash", tint = color) } }
    is Screen.Status -> { { Icon(Icons.Filled.List, contentDescription = "Status", tint = color) } }
    is Screen.NewComplaint -> { { Icon(Icons.Filled.Add, contentDescription = "New Complaint", tint = color) } }
    is Screen.Announcements -> { { Icon(Icons.Filled.Info, contentDescription = "Announcements", tint = color) } }
    is Screen.Profile -> { { Icon(Icons.Filled.Person, contentDescription = "Profile", tint = color) } }
    else -> { { Icon(Icons.Filled.Info, contentDescription = "Other", tint = color) } }
}

class MainActivity : ComponentActivity(), ProviderInstaller.ProviderInstallListener {
    private lateinit var auth: FirebaseAuth
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase first
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth
        
        // Enable Firebase persistence
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        
        // Initialize security provider
        ProviderInstaller.installIfNeededAsync(this, this)
        
        setContent {
            MaintenanceAppTheme {
                val navController = rememberNavController()
                var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentDestination = currentBackStack?.destination
                val currentScreen = maintenanceTabRowScreens.find { it.route == currentDestination?.route } ?: Screen.Status

                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route
                ) {
                    composable(route = Screen.Splash.route) {
                        SplashScreen(
                            onSplashFinished = {
                                if (auth.currentUser == null) {
                                    navController.navigate("auth") {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Status.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable("auth") {
                        AuthScreen(onAuthSuccess = {
                            isAuthenticated = true
                            navController.navigate(Screen.Status.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    }
                    composable(route = Screen.Status.route) {
                        if (isAuthenticated) {
                            // Show exit confirmation only when on the root screen (Status)
                            BackHandler {
                                android.app.AlertDialog.Builder(this@MainActivity)
                                    .setTitle("Exit App")
                                    .setMessage("Are you sure you want to exit?")
                                    .setPositiveButton("Exit") { _, _ ->
                                        finish()
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                            
                            Scaffold(
                                bottomBar = {
                                    MaintenanceTabRow(
                                        allScreens = maintenanceTabRowScreens,
                                        onTabSelected = { screen ->
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Status.route)
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        currentScreen = currentScreen
                                    )
                                }
                            ) { innerPadding ->
                                StatusScreen(modifier = Modifier.padding(innerPadding))
                            }
                        } else {
                            LaunchedEffect(Unit) {
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(route = Screen.NewComplaint.route) {
                        if (isAuthenticated) {
                            // Allow normal back navigation to Status screen
                            BackHandler {
                                navController.navigate(Screen.Status.route) {
                                    popUpTo(Screen.Status.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            
                            Scaffold(
                                bottomBar = {
                                    MaintenanceTabRow(
                                        allScreens = maintenanceTabRowScreens,
                                        onTabSelected = { screen ->
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Status.route)
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        currentScreen = currentScreen
                                    )
                                }
                            ) { innerPadding ->
                                NewComplaintScreen(modifier = Modifier.padding(innerPadding))
                            }
                        } else {
                            LaunchedEffect(Unit) {
                                navController.navigate("auth") {
                                    popUpTo(Screen.NewComplaint.route) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(route = Screen.Announcements.route) {
                        if (isAuthenticated) {
                            // Allow normal back navigation to Status screen
                            BackHandler {
                                navController.navigate(Screen.Status.route) {
                                    popUpTo(Screen.Status.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            
                            Scaffold(
                                bottomBar = {
                                    MaintenanceTabRow(
                                        allScreens = maintenanceTabRowScreens,
                                        onTabSelected = { screen ->
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Status.route)
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        currentScreen = currentScreen
                                    )
                                }
                            ) { innerPadding ->
                                AnnouncementsScreen(modifier = Modifier.padding(innerPadding))
                            }
                        } else {
                            LaunchedEffect(Unit) {
                                navController.navigate("auth") {
                                    popUpTo(Screen.Announcements.route) { inclusive = true }
                                }
                            }
                        }
                    }
                    composable(route = Screen.Profile.route) {
                        if (isAuthenticated) {
                            // Allow normal back navigation to Status screen
                            BackHandler {
                                navController.navigate(Screen.Status.route) {
                                    popUpTo(Screen.Status.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            
                            Scaffold(
                                bottomBar = {
                                    MaintenanceTabRow(
                                        allScreens = maintenanceTabRowScreens,
                                        onTabSelected = { screen ->
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Status.route)
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        currentScreen = currentScreen
                                    )
                                }
                            ) { innerPadding ->
                                ProfileScreen(
                                    onLogout = {
                                        auth.signOut()
                                        isAuthenticated = false
                                        navController.navigate("auth") {
                                            popUpTo(Screen.Profile.route) { inclusive = true }
                                        }
                                    },
                                    onDelete = {
                                        auth.currentUser?.let { user ->
                                            user.delete()?.addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    isAuthenticated = false
                                                    navController.navigate("auth") {
                                                        popUpTo(Screen.Profile.route) { inclusive = true }
                                                    }
                                                } else {
                                                    // Handle deletion failure
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Failed to delete account: ${task.exception?.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } ?: run {
                                            // User is null, just sign out
                                            auth.signOut()
                                            isAuthenticated = false
                                            navController.navigate("auth") {
                                                popUpTo(Screen.Profile.route) { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            LaunchedEffect(Unit) {
                                navController.navigate("auth") {
                                    popUpTo(Screen.Profile.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onProviderInstalled() {
        // Security provider is up-to-date
    }

    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: android.content.Intent?) {
        GoogleApiAvailability.getInstance().showErrorNotification(this, errorCode)
    }
} 