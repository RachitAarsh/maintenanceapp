package com.aarsh.maintenanceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Category(
    val name: String,
    val subCategories: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintForm(
    onCategorySelected: (String) -> Unit,
    onSubCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        Category("Hardware", listOf(
            "Monitor Display Issue",
            "Peripheral Malfunction",
            "Printer/Scanner Problem",
            "Power or Boot Failure"
        )),
        Category("Network", listOf(
            "Wi-Fi Connectivity Issue",
            "Slow Internet Speed",
            "VPN Access Problems",
            "Network Drive Not Accessible"
        )),
        Category("Software", listOf(
            "Microsoft Office Error",
            "System/Application Update Issue",
            "Login/Authentication Error",
            "App Crash or Not Responding"
        ))
    )

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSubCategory by remember { mutableStateOf<String?>(null) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedSubCategory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Category Dropdown
        ExposedDropdownMenuBox(
            expanded = expandedCategory,
            onExpandedChange = { expandedCategory = it }
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "Select Category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expandedCategory,
                onDismissRequest = { expandedCategory = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            selectedCategory = category
                            selectedSubCategory = null
                            expandedCategory = false
                            onCategorySelected(category.name)
                        }
                    )
                }
            }
        }

        // Sub-Category Dropdown
        if (selectedCategory != null) {
            ExposedDropdownMenuBox(
                expanded = expandedSubCategory,
                onExpandedChange = { expandedSubCategory = it }
            ) {
                OutlinedTextField(
                    value = selectedSubCategory ?: "Select Sub-Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sub-Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedSubCategory,
                    onDismissRequest = { expandedSubCategory = false }
                ) {
                    selectedCategory?.subCategories?.forEach { subCategory ->
                        DropdownMenuItem(
                            text = { Text(subCategory) },
                            onClick = {
                                selectedSubCategory = subCategory
                                expandedSubCategory = false
                                onSubCategorySelected(subCategory)
                            }
                        )
                    }
                }
            }
        }
    }
} 