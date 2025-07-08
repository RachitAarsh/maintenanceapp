package com.aarsh.maintenanceapp.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var announcements by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showNewAnnouncementDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newMessage by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("Normal") }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        db.collection("announcements")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                
                val announcementList = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        put("id", doc.id)
                    }
                } ?: emptyList()
                
                announcements = announcementList
            }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Announcements",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { showNewAnnouncementDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Announcement",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(announcements) { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        onDelete = { announcementId ->
                            showDeleteConfirmation = announcementId
                        }
                    )
                }
            }
        }
    }
    
    if (showNewAnnouncementDialog) {
        AlertDialog(
            onDismissRequest = { showNewAnnouncementDialog = false },
            title = { Text("New Announcement") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        label = { Text("Message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        minLines = 3
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Normal", "High").forEach { priority ->
                            FilterChip(
                                selected = newPriority == priority,
                                onClick = { newPriority = priority },
                                label = { Text(priority) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = when ((priority ?: "Normal")) {
                                        "High" -> Color(0xFFFFEBEE)
                                        "Low" -> Color(0xFFE8F5E9)
                                        "Normal" -> Color(0xFFFFF9C4)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    selectedContainerColor = when ((priority ?: "Normal")) {
                                        "High" -> Color(0xFFD32F2F)
                                        "Low" -> Color(0xFF2E7D32)
                                        "Normal" -> Color(0xFFF9A825)
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    labelColor = when ((priority ?: "Normal")) {
                                        "High" -> Color(0xFFD32F2F)
                                        "Low" -> Color(0xFF2E7D32)
                                        "Normal" -> Color(0xFFF9A825)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showNewAnnouncementDialog = false }) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank() && newMessage.isNotBlank()) {
                                val announcement = mapOf(
                                    "title" to newTitle,
                                    "message" to newMessage,
                                    "priority" to newPriority,
                                    "date" to Date(),
                                    "adminId" to auth.currentUser?.uid,
                                    "adminName" to (auth.currentUser?.displayName ?: "Admin")
                                )
                                
                                db.collection("announcements")
                                    .add(announcement)
                                    .addOnSuccessListener {
                                        newTitle = ""
                                        newMessage = ""
                                        newPriority = "Normal"
                                        showNewAnnouncementDialog = false
                                    }
                                    .addOnFailureListener { e ->
                                        // Handle error
                                    }
                            }
                        },
                        enabled = newTitle.isNotBlank() && newMessage.isNotBlank()
                    ) {
                        Text("Post")
                    }
                }
            }
        )
    }
    
    showDeleteConfirmation?.let { announcementId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Announcement") },
            text = { Text("Are you sure you want to delete this announcement?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.collection("announcements")
                            .document(announcementId)
                            .delete()
                            .addOnSuccessListener {
                                showDeleteConfirmation = null
                            }
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AnnouncementCard(
    announcement: Map<String, Any>,
    onDelete: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val auth = FirebaseAuth.getInstance()
    val isOwner = announcement["adminId"] == auth.currentUser?.uid
    val announcementId = announcement["id"] as? String
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when ((announcement["priority"] as? String) ?: "Normal") {
                "High" -> Color(0xFFFFEBEE)
                "Low" -> Color(0xFFE8F5E9)
                "Normal" -> Color(0xFFFFF9C4)
                else -> MaterialTheme.colorScheme.surface
            }
        )
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
                    text = announcement["title"] as? String ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = announcement["priority"] as? String ?: "Normal",
                        style = MaterialTheme.typography.labelMedium,
                        color = when ((announcement["priority"] as? String)?.trim()?.replaceFirstChar { it.uppercase() } ?: "Normal") {
                            "High" -> Color(0xFFD32F2F)
                            "Low" -> Color(0xFF2E7D32)
                            "Normal" -> Color(0xFFF9A825)
                            else -> Color(0xFFF9A825)
                        }
                    )
                    if (isOwner && announcementId != null) {
                        IconButton(
                            onClick = { onDelete(announcementId) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = when ((announcement["priority"] as? String) ?: "Normal") {
                                    "High" -> Color(0xFFD32F2F)
                                    "Low" -> Color(0xFF2E7D32)
                                    "Normal" -> Color(0xFFF9A825)
                                    else -> Color(0xFF1976D2)
                                }
                            )
                        }
                    }
                }
            }
            
            Text(
                text = announcement["message"] as? String ?: "No message",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Text(
                text = dateFormat.format((announcement["date"] as? Date) ?: Date()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
} 