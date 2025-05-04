package com.aarsh.maintenanceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import java.util.Locale

data class Announcement(
    val title: String = "",
    val message: String = "",
    val date: java.util.Date = java.util.Date(),
    val priority: String = "Normal" // Can be "High", "Normal", "Low"
)

@Composable
fun AnnouncementsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Announcements",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
        val db = FirebaseFirestore.getInstance()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        LaunchedEffect(Unit) {
            db.collection("announcements")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let {
                        announcements = it.toObjects(Announcement::class.java)
                    }
                }
        }

        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No announcements yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(announcements) { announcement ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (announcement.priority) {
                                "High" -> Color(0xFFFFEBEE)
                                "Low" -> Color(0xFFE8F5E9)
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
                                    text = announcement.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when (announcement.priority) {
                                        "High" -> Color(0xFFD32F2F)
                                        "Low" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Announcement",
                                    tint = when (announcement.priority) {
                                        "High" -> Color(0xFFD32F2F)
                                        "Low" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                            
                            Text(
                                text = announcement.message,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dateFormat.format(announcement.date),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = announcement.priority,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (announcement.priority) {
                                        "High" -> Color(0xFFD32F2F)
                                        "Low" -> Color(0xFF2E7D32)
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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