package com.aarsh.maintenanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
}

private val maintenanceTabRowScreens = listOf(
    Screen.Status,
    Screen.NewComplaint,
    Screen.Announcements
)

@Composable
fun MaintenanceTabRow(
    allScreens: List<Screen>,
    onTabSelected: (Screen) -> Unit,
    currentScreen: Screen
) {
    NavigationBar {
        allScreens.forEach { screen ->
            NavigationBarItem(
                icon = screen.icon,
                label = { Text(screen.text) },
                selected = currentScreen == screen,
                onClick = { onTabSelected(screen) }
            )
        }
    }
}

class MainActivity : ComponentActivity(), ProviderInstaller.ProviderInstallListener {
    private lateinit var auth: FirebaseAuth
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize security provider
        ProviderInstaller.installIfNeededAsync(this, this)
        
        // Initialize Firebase in background
        coroutineScope.launch {
            try {
                // Check Google Play Services availability
                val googleApiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this@MainActivity)
                if (resultCode != ConnectionResult.SUCCESS) {
                    withContext(Dispatchers.Main) {
                        googleApiAvailability.getErrorDialog(this@MainActivity, resultCode, 1)?.show()
                    }
                }
                
                // Initialize Firebase
                FirebaseApp.initializeApp(this@MainActivity)
                auth = Firebase.auth
                
                // Sign in anonymously if not already signed in
                if (auth.currentUser == null) {
                    try {
                        auth.signInAnonymously().await()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "Authentication failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Initialization error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        setContent {
            MaintenanceAppTheme {
                val navController = rememberNavController()
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
                                navController.navigate(Screen.Status.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(route = Screen.Status.route) {
                        Scaffold(
                            bottomBar = {
                                MaintenanceTabRow(
                                    allScreens = maintenanceTabRowScreens,
                                    onTabSelected = { screen ->
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
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
                    }
                    composable(route = Screen.NewComplaint.route) {
                        Scaffold(
                            bottomBar = {
                                MaintenanceTabRow(
                                    allScreens = maintenanceTabRowScreens,
                                    onTabSelected = { screen ->
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
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
                    }
                    composable(route = Screen.Announcements.route) {
                        Scaffold(
                            bottomBar = {
                                MaintenanceTabRow(
                                    allScreens = maintenanceTabRowScreens,
                                    onTabSelected = { screen ->
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
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