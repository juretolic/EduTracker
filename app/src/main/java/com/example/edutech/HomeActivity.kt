package com.example.edutech

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.edutech.model.AssessmentType
import com.example.edutech.model.Grade
import com.example.edutech.model.Subject
import com.example.edutech.ui.components.ProgressBar
import com.example.edutech.ui.screens.GradesScreen
import com.example.edutech.ui.screens.SubjectsScreen
import com.example.edutech.ui.screens.AddSubjectScreen
import com.example.edutech.ui.screens.AddGradeScreen
import com.example.edutech.ui.theme.EduTechTheme
import com.example.edutech.viewmodel.GradeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class HomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            EduTechTheme {
                HomeScreen(
                    auth = auth,
                    firestore = firestore,
                    onLogout = { logoutAndGoToLogin() }
                )
            }
        }
    }

    private fun logoutAndGoToLogin() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso).signOut()

        startActivity(
            Intent(this, MainActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        )
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "Grades")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduTracker - ${items[selectedItem]}") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedItem == 0,
                    onClick = {
                        selectedItem = 0
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Grades") },
                    label = { Text("Grades") },
                    selected = selectedItem == 1,
                    onClick = {
                        selectedItem = 1
                        navController.navigate("subjects") {
                            popUpTo("subjects") { inclusive = true }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                DashboardScreen()
            }
            composable("subjects") {
                SubjectsScreen(
                    onNavigateToGrades = { subjectId ->
                        navController.navigate("grades/$subjectId")
                    },
                    onNavigateToAddSubject = {
                        navController.navigate("add_subject")
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("grades/{subjectId}") { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId")
                if (subjectId != null) {
                    GradesScreen(
                        subjectId = subjectId,
                        onNavigateToAddGrade = {
                            navController.navigate("add_grade/$subjectId")
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable("add_subject") {
                AddSubjectScreen(
                    onSubjectAdded = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("add_grade/{subjectId}") { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId")
                if (subjectId != null) {
                    val viewModel: com.example.edutech.viewmodel.GradeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    AddGradeScreen(
                        subjectId = subjectId,
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    val viewModel: GradeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Calculate statistics
    val statistics = remember(uiState) {
        DashboardStatistics(
            totalSubjects = uiState.subjects.size,
            totalGrades = uiState.grades.size,
            passingSubjects = calculatePassingSubjects(uiState.subjects, uiState.grades),
            overallAverage = calculateOverallAverage(uiState.subjects, uiState.grades),
            semesterProgress = calculateSemesterProgress(uiState.subjects, uiState.grades)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Top Stats Section
        DashboardStatsSection(statistics)

        // Semester Progress
        if (uiState.subjects.isNotEmpty()) {
            SemesterProgressSection(statistics)
        }

        // Subjects at Risk
        val subjectsAtRisk = remember(uiState) {
            getSubjectsAtRisk(uiState.subjects, uiState.grades)
        }
        if (subjectsAtRisk.isNotEmpty()) {
            SubjectsAtRiskSection(subjectsAtRisk)
        }

        // Recent Activity
        if (uiState.grades.isNotEmpty()) {
            RecentActivitySection(uiState.grades, uiState.subjects)
        }

        // Subjects Overview
        SubjectsOverviewSection(uiState.subjects, uiState.grades)
    }
}

@Composable
private fun DashboardStatsSection(statistics: DashboardStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Academic Overview",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Subjects",
                    value = statistics.totalSubjects.toString(),
                    icon = Icons.Default.List
                )
                StatCard(
                    title = "Grades",
                    value = statistics.totalGrades.toString(),
                    icon = Icons.Default.Add
                )
                StatCard(
                    title = "Passing",
                    value = "${statistics.passingSubjects}/${statistics.totalSubjects}",
                    icon = Icons.Default.Check
                )
            }
        }
    }
}

@Composable
private fun SemesterProgressSection(statistics: DashboardStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Semester Progress",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = statistics.semesterProgress.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress: ${(statistics.semesterProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Average: %.1f".format(statistics.overallAverage),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun SubjectsAtRiskSection(subjectsAtRisk: List<SubjectRisk>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "⚠️ Subjects Needing Attention",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFC62828)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            subjectsAtRisk.forEach { risk ->
                SubjectRiskItem(risk)
            }
        }
    }
}

@Composable
private fun SubjectRiskItem(risk: SubjectRisk) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFCDD2)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = risk.subject.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFC62828)
            )
            Text(
                text = "Current: %.1f | Required: %.1f".format(
                    risk.currentAverage,
                    risk.subject.passingGrade
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC62828)
            )
            if (risk.neededAverage > 0) {
                Text(
                    text = "Need %.1f in remaining assessments".format(risk.neededAverage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
private fun RecentActivitySection(grades: List<Grade>, subjects: List<Subject>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            grades.sortedByDescending { it.date }
                .take(3)
                .forEach { grade ->
                    val subject = subjects.find { it.id == grade.subjectId }
                    if (subject != null) {
                        RecentGradeItem(grade, subject)
                    }
                }
        }
    }
}

@Composable
private fun RecentGradeItem(grade: Grade, subject: Subject) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = grade.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(grade.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "%.1f".format(grade.getGrade()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = getGradeColor(grade.getGrade())
                )
                Text(
                    text = "${grade.score}/${grade.maxScore}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SubjectsOverviewSection(subjects: List<Subject>, grades: List<Grade>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Subjects Overview",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (subjects.isEmpty()) {
                Text(
                    "No subjects added yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                subjects.forEach { subject ->
                    val subjectGrades = grades.filter { it.subjectId == subject.id }
                    val average = calculateSubjectAverage(subjectGrades, subject)
                    SubjectOverviewCard(subject, average)
                }
            }
        }
    }
}

@Composable
private fun SubjectOverviewCard(subject: Subject, average: Double) {
    val isPassing = average >= subject.passingGrade
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Passing: ${subject.passingGrade}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "%.1f".format(average),
                        style = MaterialTheme.typography.headlineMedium,
                        color = getGradeColor(average)
                    )
                    Text(
                        text = if (isPassing) "Passing" else "Below Passing",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isPassing) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            // Assessment Type Breakdown
            subject.weights.forEach { (typeStr, weight) ->
                val type = try {
                    AssessmentType.valueOf(typeStr)
                } catch (e: IllegalArgumentException) {
                    null
                }
                
                if (type != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Weight: ${(weight * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getGradeColor(grade: Double): Color {
    return when {
        grade >= 4.5 -> Color(0xFF2E7D32) // Green
        grade >= 3.0 -> Color(0xFF1976D2) // Blue
        else -> Color(0xFFC62828) // Red
    }
}

data class DashboardStatistics(
    val totalSubjects: Int,
    val totalGrades: Int,
    val passingSubjects: Int,
    val overallAverage: Double,
    val semesterProgress: Double
)

data class SubjectRisk(
    val subject: Subject,
    val currentAverage: Double,
    val neededAverage: Double
)

private fun calculatePassingSubjects(subjects: List<Subject>, grades: List<Grade>): Int {
    return subjects.count { subject ->
        val subjectGrades = grades.filter { it.subjectId == subject.id }
        if (subjectGrades.isNotEmpty()) {
            calculateSubjectAverage(subjectGrades, subject) >= subject.passingGrade
        } else false
    }
}

private fun getSubjectsAtRisk(subjects: List<Subject>, grades: List<Grade>): List<SubjectRisk> {
    return subjects.mapNotNull { subject ->
        val subjectGrades = grades.filter { it.subjectId == subject.id }
        if (subjectGrades.isNotEmpty()) {
            val currentAverage = calculateSubjectAverage(subjectGrades, subject)
            if (currentAverage < subject.passingGrade) {
                val neededAverage = calculateNeededAverage(subject, currentAverage)
                SubjectRisk(subject, currentAverage, neededAverage)
            } else null
        } else null
    }
}

private fun calculateSemesterProgress(
    subjects: List<Subject>,
    grades: List<Grade>
): Double {
    if (subjects.isEmpty()) return 0.0
    
    var totalProgress = 0.0
    var totalWeight = 0.0
    
    subjects.forEach { subject ->
        val subjectGrades = grades.filter { it.subjectId == subject.id }
        if (subjectGrades.isNotEmpty()) {
            val validGrades = subjectGrades.filter { it.isValid() }
            if (validGrades.isNotEmpty()) {
                // Calculate progress for each assessment type
                val typeProgress = mutableMapOf<AssessmentType, Double>()
                
                validGrades.groupBy { it.type }.forEach { (type, typeGrades) ->
                    val totalScore = typeGrades.sumOf { it.score.toDouble() }
                    val totalMaxScore = typeGrades.sumOf { it.maxScore.toDouble() }
                    if (totalMaxScore > 0) {
                        typeProgress[type] = totalScore / totalMaxScore
                    }
                }
                
                // Calculate weighted progress for the subject
                val weights = subject.weights as Map<AssessmentType, Double>
                val subjectProgress = typeProgress.entries.sumOf { (type, progress) ->
                    val weight = weights[type] ?: 0.0
                    progress * weight
                }
                
                val subjectWeight = weights.values.sum()
                if (subjectWeight > 0) {
                    totalProgress += subjectProgress
                    totalWeight += subjectWeight
                }
            }
        }
    }
    
    return if (totalWeight > 0) totalProgress / totalWeight else 0.0
}

private fun calculateNeededAverage(
    subject: com.example.edutech.model.Subject,
    currentAverage: Double
): Double {
    // This is a simplified calculation. You might want to make it more sophisticated
    // based on remaining assessments and their weights
    val remainingWeight = 1.0 - (currentAverage / 5.0)
    if (remainingWeight <= 0) return 0.0
    
    val neededPoints = subject.passingGrade - (currentAverage * (1 - remainingWeight))
    return if (neededPoints > 0) neededPoints / remainingWeight else 0.0
}

private fun calculateOverallAverage(
    subjects: List<com.example.edutech.model.Subject>,
    grades: List<com.example.edutech.model.Grade>
): Double {
    if (subjects.isEmpty()) return 0.0

    var totalWeightedSum = 0.0
    var totalWeight = 0.0

    subjects.forEach { subject ->
        val subjectGrades = grades.filter { it.subjectId == subject.id }
        if (subjectGrades.isNotEmpty()) {
            // Calculate weighted average for this subject
            val validGrades = subjectGrades.filter { it.isValid() }
            if (validGrades.isNotEmpty()) {
                val progressByType = mutableMapOf<AssessmentType, Double>()
                
                validGrades.groupBy { it.type }.forEach { (type, typeGrades) ->
                    val typeWeight = subject.weights[type.name] ?: 0.0
                    val typeTotal = typeGrades.sumOf { it.score.toDouble() }
                    val typeMax = typeGrades.sumOf { it.maxScore.toDouble() }
                    if (typeMax > 0) {
                        val typeAverage = (typeTotal / typeMax) * 5.0 // Convert to 5-point scale
                        progressByType[type] = typeAverage * typeWeight
                    }
                }
                
                val subjectWeightedSum = progressByType.values.sum()
                val subjectWeight = subject.weights.values.sum()
                
                if (subjectWeight > 0) {
                    totalWeightedSum += subjectWeightedSum
                    totalWeight += subjectWeight
                }
            }
        }
    }

    return if (totalWeight > 0) totalWeightedSum / totalWeight else 0.0
}

private fun calculateSubjectAverage(grades: List<Grade>, subject: Subject): Double {
    val validGrades = grades.filter { it.isValid() }
    if (validGrades.isEmpty()) return 0.0

    var totalWeightedSum = 0.0
    var totalWeight = 0.0

    // Debug logging
    Log.d("SubjectAverage", "Calculating average for subject: ${subject.name}")
    Log.d("SubjectAverage", "Valid grades count: ${validGrades.size}")

    // Group grades by assessment type
    validGrades.groupBy { it.type }.forEach { (type, typeGrades) ->
        val typeWeight = subject.weights[type.name] ?: 0.0
        Log.d("SubjectAverage", "Type: ${type.name}, Weight: $typeWeight, Grades: ${typeGrades.size}")
        
        if (typeWeight > 0) {
            val typeTotal = typeGrades.sumOf { it.score.toDouble() }
            val typeMax = typeGrades.sumOf { it.maxScore.toDouble() }
            if (typeMax > 0) {
                val typeAverage = (typeTotal / typeMax) * 5.0 // Convert to 5-point scale
                totalWeightedSum += typeAverage * typeWeight
                totalWeight += typeWeight
                Log.d("SubjectAverage", "Type average: $typeAverage")
            }
        }
    }

    val finalAverage = if (totalWeight > 0) totalWeightedSum / totalWeight else 0.0
    Log.d("SubjectAverage", "Final average for ${subject.name}: $finalAverage")
    return finalAverage
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(100.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
