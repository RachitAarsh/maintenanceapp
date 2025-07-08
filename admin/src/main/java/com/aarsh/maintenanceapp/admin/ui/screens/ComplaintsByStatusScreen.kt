package com.aarsh.maintenanceapp.admin.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.os.Environment
import android.widget.Toast
import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

@Composable
fun ComplaintsByStatusScreen(
    status: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val db = FirebaseFirestore.getInstance()
    var complaints by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var toDeleteId by remember { mutableStateOf<String?>(null) }
    val decodedStatus = java.net.URLDecoder.decode(status, "UTF-8")
    val context = LocalContext.current
    var isSending by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    // Sorting state
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedSubCategory by remember { mutableStateOf<String?>(null) }
    // Extract unique categories and subcategories from complaints
    val categories = complaints.mapNotNull { it["category"] as? String }.distinct().sorted()
    val subCategories = complaints.filter { (it["category"] as? String) == selectedCategory }
        .mapNotNull { it["subCategory"] as? String }.distinct().sorted()
    // Apply nested sort
    val sortedComplaints = complaints
        .let { list ->
            selectedCategory?.let { cat ->
                list.filter { (it["category"] as? String) == cat }
            } ?: list
        }
        .let { list ->
            selectedSubCategory?.let { sub ->
                list.filter { (it["subCategory"] as? String) == sub }
            } ?: list
        }
        .sortedWith(compareBy(
            { it["category"] as? String ?: "" },
            { it["subCategory"] as? String ?: "" }
        ))
    
    LaunchedEffect(decodedStatus, reloadKey) {
        try {
            db.collection("complaints")
                .whereEqualTo("status", decodedStatus)
                .get()
                .addOnSuccessListener { snapshot ->
                    val complaintsList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null) {
                            data.toMutableMap().apply {
                                put("id", doc.id)
                            }
                        } else {
                            null
                        }
                    }
                    
                    // Debug logging to check field names
                    if (complaintsList.isNotEmpty()) {
                        val firstComplaint = complaintsList.first()
                        Log.d("ComplaintsByStatusScreen", "Sample complaint fields: ${firstComplaint.keys}")
                        Log.d("ComplaintsByStatusScreen", "Category: ${firstComplaint["category"]}")
                        Log.d("ComplaintsByStatusScreen", "SubCategory: ${firstComplaint["subCategory"]}")
                        Log.d("ComplaintsByStatusScreen", "Location: ${firstComplaint["location"]}")
                        Log.d("ComplaintsByStatusScreen", "Complaint: ${firstComplaint["complaint"]}")
                    }
                    
                    complaints = complaintsList
                    isLoading = false
                    Log.d("ComplaintsByStatusScreen", "Loaded "+complaintsList.size+" complaints for status: $decodedStatus")
                }
                .addOnFailureListener { e ->
                    Log.e("ComplaintsByStatusScreen", "Error loading complaints", e)
                    isLoading = false
                }
        } catch (e: Exception) {
            Log.e("ComplaintsByStatusScreen", "Error in LaunchedEffect", e)
            isLoading = false
        }
    }
    
    if (toDeleteId != null) {
        AlertDialog(
            onDismissRequest = { toDeleteId = null },
            title = { Text("Delete Complaint") },
            text = { Text("Are you sure you want to delete this resolved complaint?") },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("complaints").document(toDeleteId!!).delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Complaint deleted", Toast.LENGTH_SHORT).show()
                            isLoading = true
                            reloadKey++
                            toDeleteId = null
                        }
                        .addOnFailureListener {
                            toDeleteId = null
                        }
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { toDeleteId = null }) { Text("Cancel") }
            }
        )
    }
    
    fun generateComplaintsPdf(): File? {
        if (complaints.isEmpty()) {
            Toast.makeText(context, "No complaints to send.", Toast.LENGTH_SHORT).show()
            return null
        }
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { textSize = 10f }
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = 40
        
        // Draw title
        canvas.drawText("$decodedStatus Complaints", 40f, y.toFloat(), titlePaint)
        y += 30
        
        // Define column positions and widths
        val col1X = 40f  // Sr No
        val col2X = 80f  // Name
        val col3X = 180f // Category
        val col4X = 280f // Location
        val col5X = 380f // Date
        val col6X = 480f // Status
        
        // Draw table header
        canvas.drawText("Sr No", col1X, y.toFloat(), headerPaint)
        canvas.drawText("Name", col2X, y.toFloat(), headerPaint)
        canvas.drawText("Category", col3X, y.toFloat(), headerPaint)
        canvas.drawText("Location", col4X, y.toFloat(), headerPaint)
        canvas.drawText("Date", col5X, y.toFloat(), headerPaint)
        canvas.drawText("Status", col6X, y.toFloat(), headerPaint)
        y += 20
        
        // Draw header separator line
        canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), Paint().apply { strokeWidth = 1f })
        y += 15
        
        complaints.forEachIndexed { idx, complaint ->
            if (y > 800) { // Start new page if needed
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40
                
                // Redraw header on new page
                canvas.drawText("$decodedStatus Complaints (continued)", 40f, y.toFloat(), titlePaint)
                y += 30
                canvas.drawText("Sr No", col1X, y.toFloat(), headerPaint)
                canvas.drawText("Name", col2X, y.toFloat(), headerPaint)
                canvas.drawText("Category", col3X, y.toFloat(), headerPaint)
                canvas.drawText("Location", col4X, y.toFloat(), headerPaint)
                canvas.drawText("Date", col5X, y.toFloat(), headerPaint)
                canvas.drawText("Status", col6X, y.toFloat(), headerPaint)
                y += 20
                canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), Paint().apply { strokeWidth = 1f })
                y += 15
            }
            
            val name = complaint["name"] as? String ?: "Unknown User"
            val category = complaint["category"] as? String ?: "-"
            val location = complaint["location"] as? String ?: "-"
            
            // Format date in dd/mm/yyyy format
            val date = when {
                complaint["timestamp"] is com.google.firebase.Timestamp -> {
                    val timestamp = complaint["timestamp"] as com.google.firebase.Timestamp
                    val date = timestamp.toDate()
                    val day = String.format("%02d", date.date)
                    val month = String.format("%02d", date.month + 1) // month is 0-based
                    val year = date.year + 1900 // year is years since 1900
                    "$day/$month/$year"
                }
                complaint["date"] is com.google.firebase.Timestamp -> {
                    val timestamp = complaint["date"] as com.google.firebase.Timestamp
                    val date = timestamp.toDate()
                    val day = String.format("%02d", date.date)
                    val month = String.format("%02d", date.month + 1) // month is 0-based
                    val year = date.year + 1900 // year is years since 1900
                    "$day/$month/$year"
                }
                complaint["date"] is String && (complaint["date"] as String).isNotBlank() -> {
                    complaint["date"] as String
                }
                else -> "-"
            }
            
            // Truncate long text to fit in columns
            val truncatedName = if (name.length > 15) name.substring(0, 12) + "..." else name
            val truncatedCategory = if (category.length > 12) category.substring(0, 9) + "..." else category
            val truncatedLocation = if (location.length > 12) location.substring(0, 9) + "..." else location
            // Date is already in dd/mm/yyyy format, no truncation needed
            
            canvas.drawText("${idx + 1}", col1X, y.toFloat(), paint)
            canvas.drawText(truncatedName, col2X, y.toFloat(), paint)
            canvas.drawText(truncatedCategory, col3X, y.toFloat(), paint)
            canvas.drawText(truncatedLocation, col4X, y.toFloat(), paint)
            canvas.drawText(date, col5X, y.toFloat(), paint)
            canvas.drawText(decodedStatus, col6X, y.toFloat(), paint)
            
            y += 15
        }
        
        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "complaints.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
            pdfDocument.close()
            return null
        }
        pdfDocument.close()
        return file
    }
    fun sendPdfByEmail(context: Context) {
        val db = FirebaseFirestore.getInstance()
        val complaintsCopy = complaints.toList()
        Toast.makeText(context, "Fetching user info...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.Main).launch {
            // Map of userId to user info
            val userInfoMap = mutableMapOf<String, Pair<String, String>>() // userId -> (email, phone)
            val userIds = complaintsCopy.mapNotNull { it["userId"] as? String }.toSet()
            for (userId in userIds) {
                try {
                    val userDoc = withContext(Dispatchers.IO) {
                        db.collection("users").document(userId).get().await()
                    }
                    val email = userDoc.getString("email") ?: "-"
                    val phone = userDoc.getString("phone") ?: "-"
                    userInfoMap[userId] = email to phone
                } catch (e: Exception) {
                    userInfoMap[userId] = "-" to "-"
                }
            }
            // Now generate the PDF with user info
            val statusTitle = when (decodedStatus.lowercase()) {
                "pending" -> "Pending Complaints"
                "in progress" -> "In Progress Complaints"
                "resolved" -> "Resolved Complaints"
                else -> "$decodedStatus Complaints"
            }
            val file = generateComplaintsPdfWithUserInfo(context, complaintsCopy, userInfoMap, statusTitle)
            if (file == null) {
                Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uri = FileProvider.getUriForFile(
                context,
                "com.aarsh.maintenanceapp.admin.fileprovider",
                file
            )
            // Pre-fill email, phone, and date from the first complaint if available
            val firstComplaint = complaintsCopy.firstOrNull()
            val firstUserId = firstComplaint?.get("userId") as? String
            val recipientEmail = userInfoMap[firstUserId]?.first?.takeIf { it != "-" } ?: ""
            val phone = userInfoMap[firstUserId]?.second?.takeIf { it != "-" } ?: ""
            val date =
                (firstComplaint?.get("timestamp") as? com.google.firebase.Timestamp)?.toDate()?.toString()
                    ?: (firstComplaint?.get("date") as? com.google.firebase.Timestamp)?.toDate()?.toString()
                    ?: (firstComplaint?.get("date") as? String)?.takeIf { it.isNotBlank() }
                    ?: ""
            val body = buildString {
                if (date.isNotBlank()) append("Date: $date\n")
                if (recipientEmail.isNotBlank()) append("Email: $recipientEmail\n")
                if (phone.isNotBlank()) append("Phone: $phone\n")
                append("\nPlease find attached the complaints PDF.")
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                if (recipientEmail.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, "$decodedStatus Complaints PDF")
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Send email..."))
        }
    }
    
    fun generateComplaintsPdfWithUserInfo(
        context: Context,
        complaints: List<Map<String, Any>>,
        userInfoMap: Map<String, Pair<String, String>>,
        statusTitle: String
    ): File? {
        if (complaints.isEmpty()) {
            Toast.makeText(context, "No complaints to send.", Toast.LENGTH_SHORT).show()
            return null
        }
        val pdfDocument = PdfDocument()
        val paint = Paint().apply { textSize = 8f } // Smaller text to fit more columns
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true }
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = 40
        
        // Draw title
        canvas.drawText(statusTitle, 40f, y.toFloat(), titlePaint)
        y += 30
        
        // Define column positions and widths for detailed table
        val col1X = 40f   // Sr No
        val col2X = 70f   // Name
        val col3X = 140f  // Category
        val col4X = 200f  // Location
        val col5X = 260f  // Email
        val col6X = 350f  // Phone
        val col7X = 420f  // Date
        val col8X = 480f  // Status
        
        // Draw table header
        canvas.drawText("Sr No", col1X, y.toFloat(), headerPaint)
        canvas.drawText("Name", col2X, y.toFloat(), headerPaint)
        canvas.drawText("Category", col3X, y.toFloat(), headerPaint)
        canvas.drawText("Location", col4X, y.toFloat(), headerPaint)
        canvas.drawText("Email", col5X, y.toFloat(), headerPaint)
        canvas.drawText("Phone", col6X, y.toFloat(), headerPaint)
        canvas.drawText("Date", col7X, y.toFloat(), headerPaint)
        canvas.drawText("Status", col8X, y.toFloat(), headerPaint)
        y += 20
        
        // Draw header separator line
        canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), Paint().apply { strokeWidth = 1f })
        y += 15
        
        complaints.forEachIndexed { idx, complaint ->
            if (y > 800) { // Start new page if needed
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40
                
                // Redraw header on new page
                canvas.drawText("$statusTitle (continued)", 40f, y.toFloat(), titlePaint)
                y += 30
                canvas.drawText("Sr No", col1X, y.toFloat(), headerPaint)
                canvas.drawText("Name", col2X, y.toFloat(), headerPaint)
                canvas.drawText("Category", col3X, y.toFloat(), headerPaint)
                canvas.drawText("Location", col4X, y.toFloat(), headerPaint)
                canvas.drawText("Email", col5X, y.toFloat(), headerPaint)
                canvas.drawText("Phone", col6X, y.toFloat(), headerPaint)
                canvas.drawText("Date", col7X, y.toFloat(), headerPaint)
                canvas.drawText("Status", col8X, y.toFloat(), headerPaint)
                y += 20
                canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), Paint().apply { strokeWidth = 1f })
                y += 15
            }
            
            val name = complaint["name"] as? String ?: "Unknown User"
            val category = complaint["category"] as? String ?: "-"
            val location = complaint["location"] as? String ?: "-"
            
            // Format date in dd/mm/yyyy format
            val date = when {
                complaint["timestamp"] is com.google.firebase.Timestamp -> {
                    val timestamp = complaint["timestamp"] as com.google.firebase.Timestamp
                    val date = timestamp.toDate()
                    val day = String.format("%02d", date.date)
                    val month = String.format("%02d", date.month + 1) // month is 0-based
                    val year = date.year + 1900 // year is years since 1900
                    "$day/$month/$year"
                }
                complaint["date"] is com.google.firebase.Timestamp -> {
                    val timestamp = complaint["date"] as com.google.firebase.Timestamp
                    val date = timestamp.toDate()
                    val day = String.format("%02d", date.date)
                    val month = String.format("%02d", date.month + 1) // month is 0-based
                    val year = date.year + 1900 // year is years since 1900
                    "$day/$month/$year"
                }
                complaint["date"] is String && (complaint["date"] as String).isNotBlank() -> {
                    complaint["date"] as String
                }
                else -> "-"
            }
            
            val userId = complaint["userId"] as? String
            val (email, phone) = userInfoMap[userId] ?: ("-" to "-")
            
            // Truncate long text to fit in columns
            val truncatedName = if (name.length > 12) name.substring(0, 9) + "..." else name
            val truncatedCategory = if (category.length > 10) category.substring(0, 7) + "..." else category
            val truncatedLocation = if (location.length > 10) location.substring(0, 7) + "..." else location
            val truncatedEmail = if (email.length > 12) email.substring(0, 9) + "..." else email
            val truncatedPhone = if (phone.length > 10) phone.substring(0, 7) + "..." else phone
            // Date is already in dd/mm/yyyy format, no truncation needed
            
            canvas.drawText("${idx + 1}", col1X, y.toFloat(), paint)
            canvas.drawText(truncatedName, col2X, y.toFloat(), paint)
            canvas.drawText(truncatedCategory, col3X, y.toFloat(), paint)
            canvas.drawText(truncatedLocation, col4X, y.toFloat(), paint)
            canvas.drawText(truncatedEmail, col5X, y.toFloat(), paint)
            canvas.drawText(truncatedPhone, col6X, y.toFloat(), paint)
            canvas.drawText(date, col7X, y.toFloat(), paint)
            canvas.drawText(complaint["status"] as? String ?: "Pending", col8X, y.toFloat(), paint)
            
            y += 15
        }
        
        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "complaints.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
            pdfDocument.close()
            return null
        }
        pdfDocument.close()
        return file
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar with back button and Save as PDF button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$decodedStatus Complaints (${complaints.size})",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        // Add Send PDF by Email button below the title
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { sendPdfByEmail(context) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSending
        ) {
            Text("Send PDF Via")
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sorting dropdowns UI
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Dropdown
            var expandedCategory by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expandedCategory = true }) {
                    Text(selectedCategory ?: "All Categories")
                }
                DropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Categories") },
                        onClick = {
                            selectedCategory = null
                            selectedSubCategory = null
                            expandedCategory = false
                        }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                selectedSubCategory = null
                                expandedCategory = false
                            }
                        )
                    }
                }
            }
            // Subcategory Dropdown
            var expandedSubCategory by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expandedSubCategory = true },
                    enabled = selectedCategory != null && subCategories.isNotEmpty()
                ) {
                    Text(selectedSubCategory ?: "All Subcategories")
                }
                DropdownMenu(
                    expanded = expandedSubCategory,
                    onDismissRequest = { expandedSubCategory = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Subcategories") },
                        onClick = {
                            selectedSubCategory = null
                            expandedSubCategory = false
                        }
                    )
                    subCategories.forEach { sub ->
                        DropdownMenuItem(
                            text = { Text(sub) },
                            onClick = {
                                selectedSubCategory = sub
                                expandedSubCategory = false
                            }
                        )
                    }
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (complaints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No $decodedStatus complaints found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All caught up!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedComplaints) { complaint ->
                    ComplaintCard(
                        complaint = complaint,
                        showDelete = decodedStatus == "Resolved",
                        onDelete = { id -> toDeleteId = id }
                    )
                }
            }
        }
    }
}

@Composable
fun ComplaintCard(
    complaint: Map<String, Any>,
    showDelete: Boolean = false,
    onDelete: (String) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val timestamp = complaint["timestamp"] as? com.google.firebase.Timestamp
    val date = timestamp?.toDate() ?: Date()
    val status = complaint["status"] as? String ?: "Pending"
    val complaintId = complaint["id"] as? String
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint["name"] as? String ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (status) {
                        "Resolved" -> Color(0xFF2E7D32)
                        "In Progress" -> Color(0xFF1976D2)
                        "Error" -> Color(0xFFC62828)
                        else -> Color(0xFFF57F17)
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = when (status) {
                            "Resolved" -> Color(0xFF2E7D32)
                            "In Progress" -> Color(0xFF1976D2)
                            "Error" -> Color(0xFFC62828)
                            else -> Color(0xFFF57F17)
                        }
                    )
                    if (showDelete && complaintId != null) {
                        IconButton(onClick = { onDelete(complaintId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Complaint", tint = Color.Red)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Location
            complaint["location"]?.let { location ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Location: $location",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Category and Subcategory
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                complaint["category"]?.let { category ->
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Category:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = category.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                complaint["subCategory"]?.let { subcategory ->
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Subcategory:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = subcategory.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Message/Complaint Description
            complaint["complaint"]?.let { message ->
                val messageText = message.toString().trim()
                if (messageText.isNotEmpty()) {
                    Text(
                        text = "Message:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Footer with date and contact info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                // Contact information
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    complaint["email"]?.let { email ->
                        Text(
                            text = "📧 $email",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    complaint["phone"]?.let { phone ->
                        Text(
                            text = "📞 $phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// Top-level function for generating complaints PDF with user info in tabular format
fun generateComplaintsPdfWithUserInfo(
    context: Context,
    complaints: List<Map<String, Any>>,
    userInfoMap: Map<String, Pair<String, String>>,
    statusTitle: String
): File? {
    if (complaints.isEmpty()) {
        Toast.makeText(context, "No complaints to send.", Toast.LENGTH_SHORT).show()
        return null
    }
    val pdfDocument = PdfDocument()
    val paint = Paint().apply { textSize = 8f } // Smaller text to fit more columns
    val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true }
    val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    var y = 40
    
    // Draw title
    canvas.drawText(statusTitle, 40f, y.toFloat(), titlePaint)
    y += 30
    
    // Define column positions and widths for detailed table
    val col1X = 40f   // Sr No
    val col2X = 70f   // Name
    val col3X = 140f  // Category
    val col4X = 200f  // Location
    val col5X = 260f  // Email
    val col6X = 350f  // Phone
    val col7X = 420f  // Date
    val col8X = 480f  // Status
    
    // Draw table header
    canvas.drawText("Sr No", col1X, y.toFloat(), headerPaint)
    canvas.drawText("Name", col2X, y.toFloat(), headerPaint)
    canvas.drawText("Category", col3X, y.toFloat(), headerPaint)
    canvas.drawText("Location", col4X, y.toFloat(), headerPaint)
    canvas.drawText("Email", col5X, y.toFloat(), headerPaint)
    canvas.drawText("Phone", col6X, y.toFloat(), headerPaint)
    canvas.drawText("Date", col7X, y.toFloat(), headerPaint)
    canvas.drawText("Status", col8X, y.toFloat(), headerPaint)
    y += 20
    
    // Draw header separator line
    canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), Paint().apply { strokeWidth = 1f })
    y += 15
    
    complaints.forEachIndexed { idx, complaint ->
        if (y > 800) { // Start new page if needed
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = 40
            
            // Redraw header on new page
            canvas.drawText("$statusTitle (continued)", 40f, y.toFloat(), titlePaint)
            y += 30
            canvas.drawText("Sr No", col1X, y.toFloat(), headerPaint)
            canvas.drawText("Name", col2X, y.toFloat(), headerPaint)
            canvas.drawText("Category", col3X, y.toFloat(), headerPaint)
            canvas.drawText("Location", col4X, y.toFloat(), headerPaint)
            canvas.drawText("Email", col5X, y.toFloat(), headerPaint)
            canvas.drawText("Phone", col6X, y.toFloat(), headerPaint)
            canvas.drawText("Date", col7X, y.toFloat(), headerPaint)
            canvas.drawText("Status", col8X, y.toFloat(), headerPaint)
            y += 20
            canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), Paint().apply { strokeWidth = 1f })
            y += 15
        }
        
        val name = complaint["name"] as? String ?: "Unknown User"
        val category = complaint["category"] as? String ?: "-"
        val location = complaint["location"] as? String ?: "-"
        
        // Format date in dd/mm/yyyy format
        val date = when {
            complaint["timestamp"] is com.google.firebase.Timestamp -> {
                val timestamp = complaint["timestamp"] as com.google.firebase.Timestamp
                val date = timestamp.toDate()
                val day = String.format("%02d", date.date)
                val month = String.format("%02d", date.month + 1) // month is 0-based
                val year = date.year + 1900 // year is years since 1900
                "$day/$month/$year"
            }
            complaint["date"] is com.google.firebase.Timestamp -> {
                val timestamp = complaint["date"] as com.google.firebase.Timestamp
                val date = timestamp.toDate()
                val day = String.format("%02d", date.date)
                val month = String.format("%02d", date.month + 1) // month is 0-based
                val year = date.year + 1900 // year is years since 1900
                "$day/$month/$year"
            }
            complaint["date"] is String && (complaint["date"] as String).isNotBlank() -> {
                complaint["date"] as String
            }
            else -> "-"
        }
        
        val userId = complaint["userId"] as? String
        val (email, phone) = userInfoMap[userId] ?: ("-" to "-")
        
        // Truncate long text to fit in columns
        val truncatedName = if (name.length > 12) name.substring(0, 9) + "..." else name
        val truncatedCategory = if (category.length > 10) category.substring(0, 7) + "..." else category
        val truncatedLocation = if (location.length > 10) location.substring(0, 7) + "..." else location
        val truncatedEmail = if (email.length > 12) email.substring(0, 9) + "..." else email
        val truncatedPhone = if (phone.length > 10) phone.substring(0, 7) + "..." else phone
        // Date is already in dd/mm/yyyy format, no truncation needed
        
        canvas.drawText("${idx + 1}", col1X, y.toFloat(), paint)
        canvas.drawText(truncatedName, col2X, y.toFloat(), paint)
        canvas.drawText(truncatedCategory, col3X, y.toFloat(), paint)
        canvas.drawText(truncatedLocation, col4X, y.toFloat(), paint)
        canvas.drawText(truncatedEmail, col5X, y.toFloat(), paint)
        canvas.drawText(truncatedPhone, col6X, y.toFloat(), paint)
        canvas.drawText(date, col7X, y.toFloat(), paint)
        canvas.drawText(complaint["status"] as? String ?: "Pending", col8X, y.toFloat(), paint)
        
        y += 15
    }
    
    pdfDocument.finishPage(page)
    val file = File(context.cacheDir, "complaints.pdf")
    try {
        pdfDocument.writeTo(FileOutputStream(file))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        pdfDocument.close()
        return null
    }
    pdfDocument.close()
    return file
} 