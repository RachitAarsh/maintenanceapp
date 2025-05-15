package com.aarsh.maintenanceapp.ui

import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aarsh.maintenanceapp.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewComplaintScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
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
            .padding(24.dp)
    ) {
        Text(
            text = "New Complaint",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        var name by remember { mutableStateOf("") }
        var complaint by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("") }
        var selectedSubCategory by remember { mutableStateOf("") }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            isError = name.isBlank()
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
                .padding(bottom = 24.dp),
            minLines = 4
        )

        Button(
            onClick = {
                if (name.isNotBlank() && selectedCategory.isNotBlank() && selectedSubCategory.isNotBlank()) {
                    val db = FirebaseFirestore.getInstance()
                    val complaintMap = hashMapOf(
                        "name" to name,
                        "complaint" to complaint,
                        "category" to selectedCategory,
                        "subCategory" to selectedSubCategory,
                        "status" to "Pending",
                        "timestamp" to Date()
                    )
                    
                    db.collection("complaints")
                        .add(complaintMap)
                        .addOnSuccessListener {
                            name = ""
                            complaint = ""
                            selectedCategory = ""
                            selectedSubCategory = ""
                            showCustomToast("Your complaint has been registered")
                        }
                } else {
                    val missingFields = mutableListOf<String>()
                    if (name.isBlank()) missingFields.add("Name")
                    if (selectedCategory.isBlank()) missingFields.add("Category")
                    if (selectedSubCategory.isBlank()) missingFields.add("Sub-Category")
                    
                    showCustomToast("Please fill in: ${missingFields.joinToString(", ")}")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Raise Ticket")
        }
    }
} 