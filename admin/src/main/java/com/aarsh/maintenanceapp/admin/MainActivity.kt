package com.aarsh.maintenanceapp.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aarsh.maintenanceapp.admin.ui.theme.MaintenanceAppAdminTheme
import com.aarsh.maintenanceapp.admin.ui.screens.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.common.ConnectionResult
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

sealed class Screen(val route: String, val icon: @Composable () -> Unit, val text: String) {
    object Splash : Screen(
        route = "splash",
        icon = { Icon(Icons.Filled.List, contentDescription = "Splash") },
        text = "Splash"
    )
    object Auth : Screen(
        route = "auth",
        icon = { Icon(Icons.Filled.Lock, contentDescription = "Auth") },
        text = "Auth"
    )
    object Dashboard : Screen(
        route = "dashboard",
        icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
        text = "Dashboard"
    )
    object Requests : Screen(
        route = "requests",
        icon = { Icon(Icons.Filled.List, contentDescription = "Requests") },
        text = "Requests"
    )
    object Announcements : Screen(
        route = "announcements",
        icon = { Icon(Icons.Filled.Notifications, contentDescription = "Announcements") },
        text = "Announcements"
    )
    object Statistics : Screen(
        route = "statistics",
        icon = { Icon(Icons.Filled.Info, contentDescription = "Statistics") },
        text = "Statistics"
    )
    object Profile : Screen(
        route = "profile",
        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
        text = "Profile"
    )
    object ComplaintsByStatus : Screen(
        route = "complaints_by_status/{status}",
        icon = { Icon(Icons.Default.List, contentDescription = "Complaints by Status") },
        text = "Complaints by Status"
    )
}

private val adminTabRowScreens = listOf(
    Screen.Dashboard,
    Screen.Requests,
    Screen.Announcements,
    Screen.Profile
)

@Composable
fun AdminTabRow(
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
    is Screen.Dashboard -> { { Icon(Icons.Default.Home, contentDescription = "Dashboard", tint = color) } }
    is Screen.Requests -> { { Icon(Icons.Default.List, contentDescription = "Requests", tint = color) } }
    is Screen.Announcements -> { { Icon(Icons.Default.Notifications, contentDescription = "Announcements", tint = color) } }
    is Screen.Statistics -> { { Icon(Icons.Default.Info, contentDescription = "Statistics", tint = color) } }
    is Screen.Profile -> { { Icon(Icons.Default.Person, contentDescription = "Profile", tint = color) } }
    is Screen.ComplaintsByStatus -> { { Icon(Icons.Default.List, contentDescription = "Complaints by Status", tint = color) } }
    else -> { { Icon(Icons.Default.Info, contentDescription = "Other", tint = color) } }
}

class MainActivity : ComponentActivity(), ProviderInstaller.ProviderInstallListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()
        
        // Enable Firebase persistence
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        // Initialize security provider
        ProviderInstaller.installIfNeededAsync(this, this)
        
        setContent {
            MaintenanceAppAdminTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onProviderInstalled() {
        // Security provider is up-to-date
    }

    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: android.content.Intent?) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        if (googleApiAvailability.isUserResolvableError(errorCode)) {
            googleApiAvailability.getErrorDialog(this, errorCode, 2404)?.show()
        } else {
            Toast.makeText(
                this,
                "This device is not supported",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2000),
        label = "Alpha Animation"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nfl_logoadminss),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alphaAnim.value)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "NFL IT Maintenance",
                color = Color(0xFF03A9F4),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alphaAnim.value)
            )
            
            Text(
                text = "Admin Portal",
                color = Color(0xFF03A9F4).copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentScreen = adminTabRowScreens.find { it.route == currentBackStack?.destination?.route }
        ?: Screen.Dashboard
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var isAdmin by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Handle back button press
    LaunchedEffect(currentBackStack?.destination?.route) {
        val currentRoute = currentBackStack?.destination?.route
        if (currentRoute != null && currentRoute != "auth" && currentRoute != "splash") {
            // Disable back button for main screens when authenticated
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.route != "auth" && destination.route != "splash") {
                    // We're on a main screen, handle back button
                }
            }
        }
    }
    
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        // Check if user is admin
                        db.collection("admins")
                            .document(currentUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                isAdmin = document.exists()
                                navController.navigate(if (isAdmin) Screen.Dashboard.route else "auth") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                            .addOnFailureListener {
                                isAdmin = false
                                navController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                    } else {
                        navController.navigate("auth") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable("auth") {
            AuthScreen(navController)
        }
        
        composable(Screen.Dashboard.route) {
            // Show exit confirmation only when on the root screen (Dashboard)
            BackHandler {
                android.app.AlertDialog.Builder(context)
                    .setTitle("Exit App")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Exit") { _, _ ->
                        (context as? ComponentActivity)?.finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            
            Scaffold(
                bottomBar = {
                    AdminTabRow(
                        allScreens = adminTabRowScreens,
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
            ) { paddingValues ->
                DashboardScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        
        composable(
            route = "complaints_by_status/{status}",
            arguments = listOf(
                androidx.navigation.navArgument("status") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "Pending"
                }
            )
        ) { backStackEntry ->
            val status = backStackEntry.arguments?.getString("status") ?: "Pending"
            ComplaintsByStatusScreen(
                status = status,
                navController = navController
            )
        }
        
        composable(Screen.Requests.route) {
            // Allow normal back navigation to Dashboard screen
            BackHandler {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            
            Scaffold(
                bottomBar = {
                    AdminTabRow(
                        allScreens = adminTabRowScreens,
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
            ) { paddingValues ->
                RequestsScreen(modifier = Modifier.padding(paddingValues))
            }
        }
        
        composable(Screen.Announcements.route) {
            // Allow normal back navigation to Dashboard screen
            BackHandler {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            
            Scaffold(
                bottomBar = {
                    AdminTabRow(
                        allScreens = adminTabRowScreens,
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
            ) { paddingValues ->
                AnnouncementsScreen()
            }
        }
        
        composable(Screen.Statistics.route) {
            // Allow normal back navigation to Dashboard screen
            BackHandler {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            
            Scaffold(
                bottomBar = {
                    AdminTabRow(
                        allScreens = adminTabRowScreens,
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
            ) { paddingValues ->
                StatisticsScreen(modifier = Modifier.padding(paddingValues))
            }
        }
        
        composable(Screen.Profile.route) {
            // Allow normal back navigation to Dashboard screen
            BackHandler {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            
            Scaffold(
                bottomBar = {
                    AdminTabRow(
                        allScreens = adminTabRowScreens,
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
            ) { paddingValues ->
                ProfileScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("auth") {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    },
                    onDelete = {
                        FirebaseAuth.getInstance().currentUser?.let { user ->
                            user.delete()?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    FirebaseAuth.getInstance().signOut()
                                    navController.navigate("auth") {
                                        popUpTo(Screen.Profile.route) { inclusive = true }
                                    }
                                } else {
                                    // Handle deletion failure
                                    Toast.makeText(
                                        context,
                                        "Failed to delete account: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } ?: run {
                            // User is null, just sign out
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("auth") {
                                popUpTo(Screen.Profile.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
} 