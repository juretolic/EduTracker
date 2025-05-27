package com.example.edutech.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edutech.model.AssessmentType
import com.example.edutech.model.Grade
import com.example.edutech.model.Subject
import com.example.edutech.viewmodel.GradeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    subjectId: String,
    onNavigateToAddGrade: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: GradeViewModel = viewModel()
) {
    val grades by viewModel.grades.collectAsState()

    LaunchedEffect(subjectId) {
        viewModel.loadGrades(subjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grades") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddGrade) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Grade"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (grades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No grades yet. Add your first grade!",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = grades,
                    key = { grade -> grade.id }
                ) { grade ->
                    GradeCard(grade = grade)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeCard(grade: Grade) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Grade: ${grade.getGrade()}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Type: ${grade.type}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Date: ${grade.date}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Description: ${grade.getGradeDescription()}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SubjectsListDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSubjectSelected: (Subject) -> Unit,
    viewModel: GradeViewModel = viewModel()
) {
    var showEditDialog by remember { mutableStateOf<Subject?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Subject?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("All Subjects") },
        text = {
            LazyColumn {
                items(subjects) { subject ->
                    SubjectListItem(
                        subject = subject,
                        onClick = { onSubjectSelected(subject) },
                        onEdit = { showEditDialog = subject },
                        onDelete = { showDeleteConfirmation = subject }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    // Edit Dialog
    showEditDialog?.let { subject ->
        EditSubjectDialog(
            subject = subject,
            onDismiss = { showEditDialog = null },
            onSubjectUpdated = { name, passingGrade ->
                viewModel.updateSubject(subject.copy(
                    name = name,
                    passingGrade = passingGrade
                ))
                showEditDialog = null
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { subject ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Subject") },
            text = { 
                Text("Are you sure you want to delete ${subject.name}? This will also delete all grades associated with this subject.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubject(subject)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubjectListItem(
    subject: Subject,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Passing Grade: ${subject.passingGrade}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EditSubjectDialog(
    subject: Subject,
    onDismiss: () -> Unit,
    onSubjectUpdated: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(subject.name) }
    var passingGrade by remember { mutableStateOf(subject.passingGrade.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subject") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passingGrade,
                    onValueChange = { passingGrade = it },
                    label = { Text("Passing Grade (%)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val passing = passingGrade.toDoubleOrNull() ?: subject.passingGrade
                    onSubjectUpdated(name, passing)
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SubjectSelector(
    subjects: List<Subject>,
    selectedSubject: Subject?,
    onSubjectSelected: (Subject) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedSubject?.name ?: "Select Subject")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            subjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text(subject.name) },
                    onClick = {
                        onSubjectSelected(subject)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GradeSummary(
    average: Double,
    isPassing: Boolean,
    subject: Subject,
    statistics: Map<String, Any> = emptyMap(),
    trend: List<Pair<Long, Double>> = emptyList()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPassing) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current Average
            Text(
                text = "Current Average",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "%.1f".format(average),
                style = MaterialTheme.typography.headlineLarge,
                color = if (isPassing) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Text(
                text = if (isPassing) "Passing" else "Below Passing Grade",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPassing) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Text(
                text = "Required: ${subject.passingGrade}",
                style = MaterialTheme.typography.bodySmall
            )

            // Grade Statistics
            if (statistics.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatisticItem("Average", "%.1f".format(statistics["average"] as Double))
                    StatisticItem("Highest", "%.1f".format(statistics["highest"] as Double))
                    StatisticItem("Lowest", "%.1f".format(statistics["lowest"] as Double))
                }

                // Grade Distribution
                val distribution = statistics["gradeDistribution"] as? Map<String, Int> ?: emptyMap()
                if (distribution.isNotEmpty()) {
                    Text(
                        text = "Grade Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        distribution.entries.sortedByDescending { it.key }.forEach { (grade, count) ->
                            StatisticItem(grade, count.toString())
                        }
                    }
                }
            }
            
            // Grade Weights
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Grade Weights",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            subject.weights.forEach { (type, weight) ->
                Text(
                    text = "${type.replace("_", " ")}: ${(weight * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GradesList(grades: List<Grade>) {
    LazyColumn {
        items(grades) { grade ->
            GradeItem(grade = grade)
        }
    }
}

@Composable
fun GradeItem(grade: Grade) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = grade.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = grade.type.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(grade.date.toDate()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.1f".format(grade.getGrade()),
                    style = MaterialTheme.typography.titleLarge,
                    color = when {
                        grade.getGrade() >= 4.5 -> Color(0xFF2E7D32)
                        grade.getGrade() >= 3.0 -> Color(0xFF1976D2)
                        else -> Color(0xFFC62828)
                    }
                )
                Text(
                    text = "${grade.score}/${grade.maxScore}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSubjectAdded: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var passingGrade by remember { mutableStateOf("60.0") }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun validateAndSubmit() {
        try {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                hasError = true
                errorMessage = "Subject name is required"
                return
            }

            val gradeValue = try {
                passingGrade.toDouble()
            } catch (e: NumberFormatException) {
                hasError = true
                errorMessage = "Please enter a valid number"
                return
            }

            if (gradeValue < 0.0 || gradeValue > 100.0) {
                hasError = true
                errorMessage = "Grade must be between 0 and 100"
                return
            }

            // If we get here, all validation passed
            onSubjectAdded(trimmedName, gradeValue)
        } catch (e: Exception) {
            hasError = true
            errorMessage = "An error occurred: ${e.message}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Subject") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        hasError = false
                        errorMessage = ""
                    },
                    label = { Text("Subject Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passingGrade,
                    onValueChange = { 
                        passingGrade = it
                        hasError = false
                        errorMessage = ""
                    },
                    label = { Text("Passing Grade (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError
                )
                if (hasError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { validateAndSubmit() }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddGradeDialog(
    onDismiss: () -> Unit,
    onGradeAdded: (Double, Double, String, AssessmentType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var maxScore by remember { mutableStateOf("100") }
    var selectedType by remember { mutableStateOf(AssessmentType.ASSIGNMENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Grade") },
        text = {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text("Score") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxScore,
                    onValueChange = { maxScore = it },
                    label = { Text("Max Score") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                AssessmentTypeSelector(
                    selected = selectedType,
                    onTypeSelected = { selectedType = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val scoreValue = score.toDoubleOrNull() ?: return@TextButton
                    val maxScoreValue = maxScore.toDoubleOrNull() ?: return@TextButton
                    onGradeAdded(scoreValue, maxScoreValue, title, selectedType)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AssessmentTypeSelector(
    selected: AssessmentType,
    onTypeSelected: (AssessmentType) -> Unit
) {
    Column {
        Text("Assessment Type", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AssessmentType.values().forEach { type ->
                Button(
                    onClick = { onTypeSelected(type) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == selected) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.surface,
                        contentColor = if (type == selected) MaterialTheme.colorScheme.onPrimary 
                                     else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(type.name)
                }
            }
        }
    }
} 