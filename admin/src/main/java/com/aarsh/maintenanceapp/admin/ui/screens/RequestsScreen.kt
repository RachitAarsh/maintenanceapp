package com.aarsh.maintenanceapp.admin.ui.screens

import android.R.attr.enabled
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedRequest by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showRequestDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    fun updateRequestStatus(requestId: String, newStatus: String, onSuccess: () -> Unit = {}) {
        db.collection("complaints")
            .document(requestId)
            .update("status", newStatus)
            .addOnSuccessListener {
                if (newStatus == "Resolved") {
                    Toast.makeText(context, "Complaint has been marked as resolved.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                val error = "Failed to update status: ${e.message}"
                Log.e("RequestsScreen", error, e)
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
    }
    
    LaunchedEffect(Unit) {
        try {
            db.collection("complaints")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        val error = "Error fetching requests: ${e.message}"
                        Log.e("RequestsScreen", error, e)
                        when (e) {
                            is FirebaseFirestoreException -> {
                                when (e.code) {
                                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                                        errorMessage = "Permission denied. Please check your authentication status."
                                        // Try to refresh auth token
                                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                    }
                                    FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                                        errorMessage = "Authentication required. Please sign in again."
                                        // Sign out and redirect to login
                                        FirebaseAuth.getInstance().signOut()
                                    }
                                    else -> errorMessage = error
                                }
                            }
                            else -> errorMessage = error
                        }
                        return@addSnapshotListener
                    }
                    
                    if (snapshot == null) {
                        Log.w("RequestsScreen", "Snapshot is null")
                        return@addSnapshotListener
                    }
                    
                    Log.d("RequestsScreen", "Received ${snapshot.documents.size} documents")
                    Log.d("RequestsScreen", "Current user: ${FirebaseAuth.getInstance().currentUser?.uid}")
                    
                    val requestList = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.data?.toMutableMap()?.apply {
                                put("id", doc.id)
                                // Convert Timestamp to Date if needed
                                get("date")?.let { timestamp ->
                                    if (timestamp is com.google.firebase.Timestamp) {
                                        put("timestamp", timestamp)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("RequestsScreen", "Error processing document ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    
                    Log.d("RequestsScreen", "Loaded ${requestList.size} requests")
                    requests = requestList
                }
        } catch (e: Exception) {
            val error = "Error setting up Firestore listener: ${e.message}"
            Log.e("RequestsScreen", error, e)
            errorMessage = error
        }
    }
    
    val filteredRequests = requests.filter { (it["status"] as? String ?: "Pending") != "Resolved" }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Requests",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFC62828)
                )
            }
        }
        
        if (filteredRequests.isEmpty() && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No requests found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRequests) { request ->
                    RequestCard(
                        request = request,
                        onStartProgress = { id -> updateRequestStatus(id, "In Progress") },
                        onMarkResolved = { id -> updateRequestStatus(id, "Resolved") },
                        onClick = {
                            selectedRequest = request
                            showRequestDialog = true
                        }
                    )
                }
            }
        }
    }
    
    if (showRequestDialog && selectedRequest != null) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("Request Details") },
            text = {
                Column {
                    Text(
                        text = "Name: ${selectedRequest?.get("name")}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Category: ${selectedRequest?.get("category")}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sub-Category: ${selectedRequest?.get("subCategory")}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Description: ${selectedRequest?.get("complaint")}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: ${selectedRequest?.get("status")}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val requestId = selectedRequest?.get("id") as? String
                    val currentStatus = selectedRequest?.get("status") as? String

                    if (currentStatus == "Pending") {
                        TextButton(
                            onClick = {
                                requestId?.let { id ->
                                    updateRequestStatus(id, "In Progress") {
                                        showRequestDialog = false
                                    }
                                }
                            }
                        ) {
                            Text("Start Progress")
                        }
                    }
                    
                    if (currentStatus == "In Progress") {
                        TextButton(
                            onClick = {
                                requestId?.let { id ->
                                    updateRequestStatus(id, "Resolved") {
                                        showRequestDialog = false
                                    }
                                }
                            }
                        ) {
                            Text("Mark Resolved")
                        }
                    }
                    
                    TextButton(onClick = { showRequestDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
fun RequestCard(
    request: Map<String, Any>,
    onStartProgress: (String) -> Unit,
    onMarkResolved: (String) -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val timestamp = request["date"] as? com.google.firebase.Timestamp
    val date = timestamp?.toDate() ?: Date()
    val status = request["status"] as? String ?: "Pending"
    val requestId = request["id"] as? String ?: return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                "Resolved" -> Color(0xFFE8F5E9)
                "In Progress" -> Color(0xFFE3F2FD)
                "Error" -> Color(0xFFFFEBEE)
                else -> Color(0xFFFFF8E1)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // User Name and Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request["name"] as? String ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormat.format(date),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Location
            Text(
                text = "Location: ${request["location"] as? String ?: "Not specified"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Category and Sub-Category
            Text(
                text = "Category: ${request["category"] as? String ?: "Not specified"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Sub-Category: ${request["subCategory"] as? String ?: "Not specified"}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Complaint Message
            val complaintMessage = request["complaint"] as? String
            if (!complaintMessage.isNullOrEmpty()) {
                Text(
                    text = "Message: $complaintMessage",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Status and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (status) {
                        "Resolved" -> Color(0xFF2E7D32)
                        "In Progress" -> Color(0xFF1976D2)
                        "Error" -> Color(0xFFC62828)
                        else -> Color(0xFFF57F17)
                    }
                )
                
                Row {
                    if (status == "Pending") {
                        TextButton(
                            onClick = { onStartProgress(requestId) }
                        ) {
                            Text("Start Progress")
                        }
                    }
                    if (status == "In Progress") {
                        TextButton(
                            onClick = { onMarkResolved(requestId) }
                        ) {
                            Text("Mark Resolved")
                        }
                    }
                }
            }
        }
    }
}