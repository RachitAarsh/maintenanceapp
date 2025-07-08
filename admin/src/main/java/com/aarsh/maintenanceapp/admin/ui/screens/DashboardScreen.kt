package com.aarsh.maintenanceapp.admin.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DashboardScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val db = FirebaseFirestore.getInstance()
    var pendingCount by remember { mutableStateOf(0) }
    var inProgressCount by remember { mutableStateOf(0) }
    var resolvedCount by remember { mutableStateOf(0) }
    var totalRequests by remember { mutableStateOf(0) }
    var categoryStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    LaunchedEffect(Unit) {
        try {
            // Get total requests
            db.collection("complaints")
                .get()
                .addOnSuccessListener { snapshot ->
                    totalRequests = snapshot.size()
                    Log.d("DashboardScreen", "Total requests: $totalRequests")
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardScreen", "Error getting total requests", e)
                }
            
            // Get status counts
            db.collection("complaints")
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener { snapshot ->
                    pendingCount = snapshot.size()
                    Log.d("DashboardScreen", "Pending requests: $pendingCount")
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardScreen", "Error getting pending requests", e)
                }
            
            db.collection("complaints")
                .whereEqualTo("status", "In Progress")
                .get()
                .addOnSuccessListener { snapshot ->
                    inProgressCount = snapshot.size()
                    Log.d("DashboardScreen", "In Progress requests: $inProgressCount")
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardScreen", "Error getting in progress requests", e)
                }
            
            db.collection("complaints")
                .whereEqualTo("status", "Resolved")
                .get()
                .addOnSuccessListener { snapshot ->
                    resolvedCount = snapshot.size()
                    Log.d("DashboardScreen", "Resolved requests: $resolvedCount")
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardScreen", "Error getting resolved requests", e)
                }
            
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
                    Log.d("DashboardScreen", "Category stats: $categoryStats")
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardScreen", "Error getting category stats", e)
                }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error in LaunchedEffect", e)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Status Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                title = "Pending",
                count = pendingCount,
                color = Color(0xFFFFC107),
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("complaints_by_status/Pending")
                }
            )
            StatusCard(
                title = "In Progress",
                count = inProgressCount,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("complaints_by_status/In%20Progress")
                }
            )
            StatusCard(
                title = "Resolved",
                count = resolvedCount,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("complaints_by_status/Resolved")
                }
            )
        }
        
        // Category Distribution Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (categoryStats.isNotEmpty()) {
                    CategoryPieChart(
                        categoryStats = categoryStats,
                        totalRequests = totalRequests,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun CategoryPieChart(
    categoryStats: Map<String, Int>,
    totalRequests: Int,
    modifier: Modifier = Modifier
) {
    val pieColors = listOf(
        android.graphics.Color.rgb(76, 175, 80),   // Green
        android.graphics.Color.rgb(255, 235, 59), // Yellow
        android.graphics.Color.rgb(244, 67, 54)   // Red
    )
    Column(modifier = modifier) {
        // Summary statistics at the top
        if (categoryStats.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val mostCommonCategory = categoryStats.maxByOrNull { it.value }
                    if (mostCommonCategory != null) {
                        StatisticItem(
                            label = "Most Common",
                            value = mostCommonCategory.key,
                            subtitle = "${mostCommonCategory.value} complaints"
                        )
                    } else {
                        StatisticItem(
                            label = "Most Common",
                            value = "None",
                            subtitle = "0 complaints"
                        )
                    }
                    StatisticItem(
                        label = "Categories",
                        value = categoryStats.size.toString(),
                        subtitle = "Total types"
                    )
                    val averagePerCategory = if (categoryStats.isNotEmpty()) totalRequests / categoryStats.size else 0
                    StatisticItem(
                        label = "Average",
                        value = averagePerCategory.toString(),
                        subtitle = "Per category"
                    )
                }
            }
        }
        // Chart container
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            factory = { context ->
                PieChart(context).apply {
                    description.isEnabled = false
                    setUsePercentValues(true)
                    legend.isEnabled = false // Disable legend since we'll show labels on slices
                    setEntryLabelTextSize(8f)
                    setEntryLabelColor(android.graphics.Color.BLACK)
                    setDrawEntryLabels(true)
                    setDrawHoleEnabled(true)
                    holeRadius = 50f
                    setHoleColor(android.graphics.Color.TRANSPARENT)
                    setTransparentCircleRadius(53f)
                    setDrawCenterText(true)
                    centerText = "Total\n$totalRequests"
                    setCenterTextSize(16f)
                    setCenterTextColor(android.graphics.Color.BLACK)
                    setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD)
                    animateY(1400)
                    animateX(1400)
                    setTouchEnabled(true)
                    setRotationEnabled(true)
                    setHighlightPerTapEnabled(true)
                    setExtraOffsets(5f, 5f, 5f, 5f)
                }
            },
            update = { chart ->
                val entries = mutableListOf<PieEntry>()
                val colors = mutableListOf<Int>()
                categoryStats.forEach { (category, count) ->
                    val percentage = if (totalRequests > 0) (count.toFloat() / totalRequests * 100) else 0f
                    entries.add(PieEntry(percentage, "$category\n$count"))
                }
                repeat(categoryStats.size) { index ->
                    colors.add(pieColors[index % pieColors.size])
                }
                val dataSet = PieDataSet(entries, "Categories").apply {
                    this.colors = colors
                    valueTextSize = 12f
                    valueTextColor = android.graphics.Color.BLACK
                    valueFormatter = PercentFormatter(chart)
                    valueLinePart1Length = 0.3f
                    valueLinePart2Length = 0.3f
                    valueLineColor = android.graphics.Color.GRAY
                    yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                    valueLineWidth = 1f
                    sliceSpace = 2f
                    selectionShift = 8f
                }
                val pieData = PieData(dataSet)
                chart.data = pieData
                chart.invalidate()
            }
        )
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
} 