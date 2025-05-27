package com.example.edutech.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edutech.viewmodel.SubjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectScreen(
    onSubjectAdded: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SubjectViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var passingGrade by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Subject") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    showError = false
                },
                label = { Text("Subject Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = showError && name.isBlank()
            )

            OutlinedTextField(
                value = passingGrade,
                onValueChange = { 
                    passingGrade = it
                    showError = false
                },
                label = { Text("Passing Grade (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                isError = showError && (passingGrade.isBlank() || passingGrade.toDoubleOrNull() == null)
            )

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    when {
                        name.isBlank() -> {
                            showError = true
                            errorMessage = "Please enter a subject name"
                        }
                        passingGrade.isBlank() -> {
                            showError = true
                            errorMessage = "Please enter a passing grade"
                        }
                        else -> {
                            val grade = passingGrade.toDoubleOrNull()
                            if (grade == null) {
                                showError = true
                                errorMessage = "Please enter a valid number"
                            } else if (grade < 0.0 || grade > 100.0) {
                                showError = true
                                errorMessage = "Grade must be between 0 and 100"
                            } else {
                                viewModel.addSubject(name, grade)
                                onSubjectAdded()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Add Subject")
            }
        }
    }
} 