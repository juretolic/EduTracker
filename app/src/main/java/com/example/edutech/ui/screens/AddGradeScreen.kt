package com.example.edutech.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edutech.model.AssessmentType
import com.example.edutech.viewmodel.GradeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGradeScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    viewModel: GradeViewModel
) {
    var title by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var maxScore by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AssessmentType.PRE_EXAM) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    val selectedSubject = uiState.selectedSubject
    
    // Set the selected subject when the screen is created
    LaunchedEffect(subjectId) {
        Log.d("AddGradeScreen", "Setting up subject selection for ID: $subjectId")
        viewModel.selectSubjectById(subjectId)
    }
    
    // Show error if subject selection fails
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Log.e("AddGradeScreen", "Error from ViewModel: $error")
            errorMessage = error
            showError = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = selectedSubject?.name?.let { "Add Grade - $it" } 
                            ?: "Add Grade"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedSubject == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Show loading indicator if needed
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        showError = false
                    },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    isError = showError && title.isBlank(),
                    enabled = uiState.selectedSubject != null
                )

                OutlinedTextField(
                    value = score,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            score = it
                            showError = false
                        }
                    },
                    label = { Text("Score") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = showError && (score.isBlank() || score.toDoubleOrNull() == null),
                    enabled = uiState.selectedSubject != null
                )

                OutlinedTextField(
                    value = maxScore,
                    onValueChange = { 
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            maxScore = it
                            showError = false
                        }
                    },
                    label = { Text("Max Score") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    isError = showError && (maxScore.isBlank() || maxScore.toDoubleOrNull() == null),
                    enabled = uiState.selectedSubject != null
                )

                Text(
                    text = "Assessment Type",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssessmentType.values().forEach { type ->
                        FilterChip(
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            label = { Text(type.name.replace("_", " ")) },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.selectedSubject != null
                        )
                    }
                }

                Button(
                    onClick = {
                        if (uiState.selectedSubject == null) {
                            showError = true
                            errorMessage = "Please select a subject first"
                            return@Button
                        }

                        val scoreValue = score.toDoubleOrNull()
                        val maxScoreValue = maxScore.toDoubleOrNull()

                        when {
                            title.isBlank() -> {
                                showError = true
                                errorMessage = "Please enter a title"
                            }
                            scoreValue == null -> {
                                showError = true
                                errorMessage = "Please enter a valid score"
                            }
                            maxScoreValue == null -> {
                                showError = true
                                errorMessage = "Please enter a valid max score"
                            }
                            scoreValue < 0 || scoreValue > maxScoreValue -> {
                                showError = true
                                errorMessage = "Score must be between 0 and max score"
                            }
                            else -> {
                                Log.d("AddGradeScreen", "Adding grade with score=$scoreValue, maxScore=$maxScoreValue, title=$title, type=$selectedType")
                                viewModel.addGrade(scoreValue, maxScoreValue, title, selectedType)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = uiState.selectedSubject != null && !uiState.isLoading
                ) {
                    Text("Add Grade")
                }
            }
        }
    }
    
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
} 