package com.aarsh.maintenanceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aarsh.maintenanceapp.ui.theme.MaintenanceBackground
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        var complaints by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        val db = FirebaseFirestore.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        LaunchedEffect(Unit) {
            db.collection("complaints")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let {
                        complaints = it.documents.map { doc -> 
                            doc.data ?: emptyMap()
                        }
                    }
                }
        }

        if (complaints.isEmpty()) {
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
                                    text = complaint["name"] as? String ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Color(0xFF2E7D32)
                                        "Error" -> Color(0xFFD32F2F)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Icon(
                                    imageVector = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Icons.Default.CheckCircle
                                        "Error" -> Icons.Default.Warning
                                        else -> Icons.Default.Warning
                                    },
                                    contentDescription = "Status Icon",
                                    tint = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Color(0xFF2E7D32)
                                        "Error" -> Color(0xFFD32F2F)
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
                                    text = dateFormat.format((complaint["timestamp"] as? com.google.firebase.Timestamp)?.toDate() ?: Date()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = complaint["status"] as? String ?: "Pending",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (complaint["status"] as? String ?: "Pending") {
                                        "Resolved" -> Color(0xFF2E7D32)
                                        "Error" -> Color(0xFFD32F2F)
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