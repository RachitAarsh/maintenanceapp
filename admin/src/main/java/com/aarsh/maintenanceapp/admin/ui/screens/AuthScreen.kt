package com.aarsh.maintenanceapp.admin.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aarsh.maintenanceapp.admin.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import com.aarsh.maintenanceapp.admin.navigation.Screen
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // NFL Logo
        Image(
            painter = painterResource(id = R.drawable.ic_nfl_logo),
            contentDescription = "NFL Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(vertical = 16.dp)
        )

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF1976D2),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF1976D2)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Sign Up") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Login") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card for form
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedTab == 0) {
                    // Sign Up Form
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1976D2),
                            focusedLabelColor = Color(0xFF1976D2)
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF1976D2),
                            focusedLabelColor = Color(0xFF1976D2)
                        )
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color(0xFF1976D2)
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color(0xFF1976D2)
                    )
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            // Sign Up
                            if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
                                errorMessage = "Please fill all fields"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = ""
                            
                            println("Attempting to create admin account for: $email")
                            
                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val adminMap = hashMapOf(
                                            "name" to name,
                                            "email" to email,
                                            "phone" to phone,
                                            "createdAt" to com.google.firebase.Timestamp.now()
                                        )
                                        task.result.user?.let { user ->
                                            println("User created with UID: ${user.uid}")
                                            FirebaseFirestore.getInstance().collection("admins")
                                                .document(user.uid)
                                                .set(adminMap)
                                                .addOnSuccessListener {
                                                    println("Admin document created successfully")
                                                    navController.navigate(Screen.Dashboard.route) {
                                                        popUpTo(0) { inclusive = true }
                                                        launchSingleTop = true
                                                    }
                                                }
                                                .addOnFailureListener { exception ->
                                                    println("Failed to create admin document: ${exception.message}")
                                                    errorMessage = "Account created but failed to set up admin profile: ${exception.message}"
                                                    isLoading = false
                                                }
                                        } ?: run {
                                            println("No user returned from account creation")
                                            errorMessage = "Failed to create user account"
                                            isLoading = false
                                        }
                                    } else {
                                        val exception = task.exception
                                        println("Account creation failed: ${exception?.message}")
                                        when (exception) {
                                            is FirebaseAuthWeakPasswordException -> {
                                                errorMessage = "Password is too weak. Please use at least 6 characters."
                                            }
                                            is FirebaseAuthInvalidCredentialsException -> {
                                                errorMessage = "Invalid email format."
                                            }
                                            is FirebaseAuthUserCollisionException -> {
                                                errorMessage = "An account with this email already exists."
                                            }
                                            else -> {
                                                errorMessage = exception?.message ?: "Sign up failed"
                                            }
                                        }
                                        isLoading = false
                                    }
                                }
                        } else {
                            // Login
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill all fields"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = ""
                            
                            // Add some debugging
                            println("Attempting login with email: $email")
                            
                            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        println("Firebase Auth successful, checking admin status...")
                                        // Check if user is an admin
                                        task.result.user?.let { user ->
                                            println("User UID: ${user.uid}")
                                            FirebaseFirestore.getInstance().collection("admins")
                                                .document(user.uid)
                                                .get()
                                                .addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        println("Admin document found, navigating to dashboard")
                                                        navController.navigate(Screen.Dashboard.route) {
                                                            popUpTo(0) { inclusive = true }
                                                            launchSingleTop = true
                                                        }
                                                    } else {
                                                        println("Admin document not found for UID: ${user.uid}")
                                                        errorMessage = "Access denied. This account is not registered as an admin. Please contact the system administrator."
                                                        FirebaseAuth.getInstance().signOut()
                                                        isLoading = false
                                                    }
                                                }
                                                .addOnFailureListener { exception ->
                                                    println("Failed to check admin status: ${exception.message}")
                                                    errorMessage = "Failed to verify admin status: ${exception.message}"
                                                    isLoading = false
                                                }
                                        } ?: run {
                                            println("No user returned from Firebase Auth")
                                            errorMessage = "Login failed: No user returned"
                                            isLoading = false
                                        }
                                    } else {
                                        val exception = task.exception
                                        println("Firebase Auth failed: ${exception?.message}")
                                        when (exception) {
                                            is FirebaseAuthInvalidCredentialsException -> {
                                                errorMessage = "Invalid email or password. Please check your credentials."
                                            }
                                            is FirebaseAuthInvalidUserException -> {
                                                errorMessage = "No account found with this email address."
                                            }
                                            else -> {
                                                val errorMessageText = exception?.message ?: "Login failed. Please check your internet connection and try again."
                                                if (errorMessageText.contains("too many requests", ignoreCase = true)) {
                                                    errorMessage = "Too many failed login attempts. Please try again later."
                                                } else if (errorMessageText.contains("network", ignoreCase = true)) {
                                                    errorMessage = "Network error. Please check your internet connection and try again."
                                                } else {
                                                    errorMessage = errorMessageText
                                                }
                                            }
                                        }
                                        isLoading = false
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = if (selectedTab == 0) "Sign Up" else "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
} 