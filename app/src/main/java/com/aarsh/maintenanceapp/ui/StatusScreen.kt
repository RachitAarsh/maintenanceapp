package com.aarsh.maintenanceapp.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarsh.maintenanceapp.ui.theme.MaintenanceBackground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext

@Composable
fun StatusScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        var complaints by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            if (currentUser != null) {
                try {
                    Log.d("StatusScreen", "Setting up Firestore listener for user: ${currentUser.uid}")
                    db.collection("complaints")
                        .whereEqualTo("userId", currentUser.uid)
                        .addSnapshotListener { snapshot, e ->
                            isLoading = false
                            if (e != null) {
                                Log.e("StatusScreen", "Error fetching complaints", e)
                                errorMessage = "Error loading complaints: ${e.message}"
                                return@addSnapshotListener
                            }
                            
                            snapshot?.let {
                                Log.d("StatusScreen", "Received ${it.documents.size} complaints")
                                // Convert documents to list and sort by date
                                val sortedComplaints = it.documents.mapNotNull { doc -> 
                                    doc.data?.toMutableMap()?.apply {
                                        put("id", doc.id)
                                        // Convert Timestamp to Date if needed
                                        get("date")?.let { timestamp ->
                                            if (timestamp is com.google.firebase.Timestamp) {
                                                put("date", timestamp.toDate())
                                            }
                                        }
                                    }
                                }.sortedByDescending { complaint ->
                                    (complaint["date"] as? Date) ?: Date(0)
                                }
                                complaints = sortedComplaints
                                Log.d("StatusScreen", "Processed ${sortedComplaints.size} complaints")
                            }
                        }
                } catch (e: Exception) {
                    Log.e("StatusScreen", "Error setting up Firestore listener", e)
                    errorMessage = "Error setting up complaint listener: ${e.message}"
                    isLoading = false
                }
            } else {
                Log.w("StatusScreen", "No current user found")
                errorMessage = "Please sign in to view your complaints"
                isLoading = false
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (complaints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No complaints yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(complaints) { complaint ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (complaint["status"] as? String ?: "Pending") {
                                "Resolved" -> Color(0xFFE8F5E9)
                                "Error" -> Color(0xFFFFEBEE)
                                "In Progress" -> Color(0xFFE3F2FD)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = complaint["location"] as? String ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Color(0xFF2E7D32)
                                        "Error" -> Color(0xFFD32F2F)
                                        "In Progress" -> Color(0xFF1976D2)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Icon(
                                    imageVector = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Icons.Default.CheckCircle
                                        "Error" -> Icons.Default.Warning
                                        "In Progress" -> Icons.Default.Info
                                        else -> Icons.Default.Warning
                                    },
                                    contentDescription = "Status Icon",
                                    tint = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Color(0xFF2E7D32)
                                        "Error" -> Color(0xFFD32F2F)
                                        "In Progress" -> Color(0xFF1976D2)
                                        else -> Color(0xFFFFC107)
                                    }
                                )
                            }
                            
                            // Category and Sub-Category Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "${complaint["category"] as? String ?: ""} - ${complaint["subCategory"] as? String ?: ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            Text(
                                text = complaint["complaint"] as? String ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dateFormat.format((complaint["date"] as? Date) ?: Date()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = complaint["status"] as? String ?: "Pending",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Color(0xFF2E7D32)
                                        "Error" -> Color(0xFFD32F2F)
                                        "In Progress" -> Color(0xFF1976D2)
                                        else -> Color(0xFFFFC107)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 