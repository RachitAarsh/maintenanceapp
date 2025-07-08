package com.aarsh.maintenanceapp.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier) {
    val db = FirebaseFirestore.getInstance()
    var totalRequests by remember { mutableStateOf(0) }
    var pendingRequests by remember { mutableStateOf(0) }
    var inProgressRequests by remember { mutableStateOf(0) }
    var resolvedRequests by remember { mutableStateOf(0) }
    var categoryStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var recentActivity by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        // Get total requests
        db.collection("complaints")
            .get()
            .addOnSuccessListener { totalRequests = it.size() }
        
        // Get status counts
        db.collection("complaints")
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { pendingRequests = it.size() }
        
        db.collection("complaints")
            .whereEqualTo("status", "In Progress")
            .get()
            .addOnSuccessListener { inProgressRequests = it.size() }
        
        db.collection("complaints")
            .whereEqualTo("status", "Resolved")
            .get()
            .addOnSuccessListener { resolvedRequests = it.size() }
        
        // Get category statistics
        db.collection("complaints")
            .get()
            .addOnSuccessListener { snapshot ->
                val stats = mutableMapOf<String, Int>()
                snapshot.documents.forEach { doc ->
                    val category = doc.getString("category") ?: "Other"
                    stats[category] = (stats[category] ?: 0) + 1
                }
                categoryStats = stats
            }
        
        // Get recent activity
        db.collection("complaints")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    recentActivity = it.documents.mapNotNull { doc -> doc.data }
                }
            }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Status Overview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status Overview",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Total",
                        value = totalRequests,
                        color = Color(0xFF1976D2)
                    )
                    StatItem(
                        label = "Pending",
                        value = pendingRequests,
                        color = Color(0xFFFFC107)
                    )
                    StatItem(
                        label = "In Progress",
                        value = inProgressRequests,
                        color = Color(0xFF2196F3)
                    )
                    StatItem(
                        label = "Resolved",
                        value = resolvedRequests,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
        
        // Category Distribution
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                categoryStats.forEach { (category, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (totalRequests > 0) {
                                "$count (${(count.toFloat() / totalRequests * 100).toInt()}%)"
                            } else {
                                "$count (0%)"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Recent Activity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                recentActivity.forEach { activity ->
                    ActivityItem(activity = activity)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ActivityItem(activity: Map<String, Any>) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = activity["name"] as? String ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = activity["complaint"] as? String ?: "No description",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        Text(
            text = try {
                val timestamp = activity["timestamp"] as? com.google.firebase.Timestamp
                if (timestamp != null) {
                    dateFormat.format(timestamp.toDate())
                } else {
                    "Unknown date"
                }
            } catch (e: Exception) {
                "Invalid date"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
} 