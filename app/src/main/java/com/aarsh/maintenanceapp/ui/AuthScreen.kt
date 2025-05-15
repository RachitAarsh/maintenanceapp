package com.aarsh.maintenanceapp.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarsh.maintenanceapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Sign Up", "Login")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_nfl_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NFL IT Maintenance",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF388E3C)
        )
        Text(
            text = "Sign up or log in to continue",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                if (selectedTab == 0) {
                    // Sign Up
                    var name by remember { mutableStateOf("") }
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var phone by remember { mutableStateOf("") }
                    var loading by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf("") }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            singleLine = true
                        )
                        if (error.isNotEmpty()) {
                            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Button(
                            onClick = {
                                error = ""
                                if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
                                    error = "Please fill all fields"
                                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    error = "Invalid email address"
                                } else {
                                    loading = true
                                    val auth = FirebaseAuth.getInstance()
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            loading = false
                                            if (task.isSuccessful) {
                                                val user = auth.currentUser
                                                val db = FirebaseFirestore.getInstance()
                                                val userMap = hashMapOf(
                                                    "uid" to (user?.uid ?: ""),
                                                    "name" to name,
                                                    "email" to email,
                                                    "phone" to phone
                                                )
                                                db.collection("users").document(user?.uid ?: "").set(userMap)
                                                Toast.makeText(context, "Sign up successful!", Toast.LENGTH_SHORT).show()
                                                onAuthSuccess()
                                            } else {
                                                error = "Sign up failed: ${task.exception?.message}" 
                                            }
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading
                        ) {
                            Text(
                                text = if (loading) "Signing Up..." else "Sign Up",
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Login
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var loading by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf("") }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            singleLine = true
                        )
                        if (error.isNotEmpty()) {
                            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Button(
                            onClick = {
                                error = ""
                                if (email.isBlank() || password.isBlank()) {
                                    error = "Please fill all fields"
                                } else {
                                    loading = true
                                    val auth = FirebaseAuth.getInstance()
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            loading = false
                                            if (task.isSuccessful) {
                                                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                                                onAuthSuccess()
                                            } else {
                                                error = "Login failed: ${task.exception?.message}" 
                                            }
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading
                        ) {
                            Text(
                                text = if (loading) "Logging In..." else "Login",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
} 