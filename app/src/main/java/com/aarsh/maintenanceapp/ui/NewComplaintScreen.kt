package com.aarsh.maintenanceapp.ui

import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aarsh.maintenanceapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import androidx.compose.ui.graphics.Color
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewComplaintScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val db = FirebaseFirestore.getInstance()
    var userName by remember { mutableStateOf("") }
    
    // Fetch user's name when the screen loads
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    userName = document.getString("name") ?: ""
                }
        }
    }
    
    fun showCustomToast(message: String) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.custom_toast_layout, null)
        val textView = layout.findViewById<TextView>(R.id.toast_text)
        textView.text = message
        
        Toast(context).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "New Complaint",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        var location by remember { mutableStateOf("") }
        var complaint by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("") }
        var selectedSubCategory by remember { mutableStateOf("") }

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            isError = location.isBlank()
        )

        ComplaintForm(
            onCategorySelected = { category ->
                selectedCategory = category
            },
            onSubCategorySelected = { subCategory ->
                selectedSubCategory = subCategory
            }
        )

        OutlinedTextField(
            value = complaint,
            onValueChange = { complaint = it },
            label = { Text("Complaint (Optional)") },
            leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = "Complaint") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(bottom = 16.dp),
            minLines = 4
        )

        Button(
            onClick = {
                if (currentUser == null) {
                    showCustomToast("Please sign in to raise a complaint")
                    return@Button
                }
                
                if (location.isNotBlank() && selectedCategory.isNotBlank() && selectedSubCategory.isNotBlank()) {
                    val complaintMap = hashMapOf(
                        "name" to userName,
                        "location" to location,
                        "complaint" to complaint,
                        "category" to selectedCategory,
                        "subCategory" to selectedSubCategory,
                        "status" to "Pending",
                        "date" to Date(),
                        "userId" to currentUser.uid
                    )
                    
                    val complaintsCollection = db.collection("complaints")
                    complaintsCollection.add(complaintMap)
                        .addOnSuccessListener {
                            location = ""
                            complaint = ""
                            selectedCategory = ""
                            selectedSubCategory = ""
                            showCustomToast("Complaint registered successfully")
                        }
                        .addOnFailureListener { e ->
                            showCustomToast("Failed to register complaint: ${e.message}")
                            Log.e("NewComplaintScreen", "Error adding complaint", e)
                        }
                } else {
                    val missingFields = mutableListOf<String>()
                    if (location.isBlank()) missingFields.add("Location")
                    if (selectedCategory.isBlank()) missingFields.add("Category")
                    if (selectedSubCategory.isBlank()) missingFields.add("Sub-Category")
                    
                    showCustomToast("Please fill in: ${missingFields.joinToString(", ")}")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Raise Ticket", color = Color.White)
        }
    }
}

