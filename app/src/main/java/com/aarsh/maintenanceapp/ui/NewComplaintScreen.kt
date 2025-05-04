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
import androidx.compose.ui.res.painterResource
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
        
        with(Toast(context)) {
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
        var selectedCategory by remember { mutableStateOf("Network") }
        var expanded by remember { mutableStateOf(false) }
        val categories = listOf("Network", "Hardware", "Software")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = { },
                readOnly = true,
                label = { Text("Category") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(
                            id = when (selectedCategory) {
                                "Hardware" -> R.drawable.ic_hardware
                                "Software" -> R.drawable.ic_software
                                "Network" -> R.drawable.ic_network
                                else -> R.drawable.ic_network
                            }
                        ),
                        contentDescription = "Category Icon"
                    )
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = when (category) {
                                            "Hardware" -> R.drawable.ic_hardware
                                            "Software" -> R.drawable.ic_software
                                            "Network" -> R.drawable.ic_network
                                            else -> R.drawable.ic_network
                                        }
                                    ),
                                    contentDescription = "Category Icon"
                                )
                                Text(category)
                            }
                        },
                        onClick = { 
                            selectedCategory = category
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = complaint,
            onValueChange = { complaint = it },
            label = { Text("Complaint") },
            leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = "Complaint") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(bottom = 24.dp),
            minLines = 4
        )

        Button(
            onClick = {
                if (name.isNotBlank() && complaint.isNotBlank()) {
                    val db = FirebaseFirestore.getInstance()
                    val complaintMap = hashMapOf(
                        "name" to name,
                        "complaint" to complaint,
                        "category" to selectedCategory,
                        "status" to "Pending",
                        "timestamp" to Date()
                    )
                    
                    db.collection("complaints")
                        .add(complaintMap)
                        .addOnSuccessListener {
                            name = ""
                            complaint = ""
                            selectedCategory = "Network"
                            expanded = false
                            showCustomToast("Your complaint has been registered")
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Raise Ticket")
        }
    }
} 