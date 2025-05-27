package com.example.edutech.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutech.model.AssessmentType
import com.example.edutech.model.Grade
import com.example.edutech.model.Subject
import com.example.edutech.repository.GradeRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class GradeUiState(
    val subjects: List<Subject> = emptyList(),
    val selectedSubject: Subject? = null,
    val grades: List<Grade> = emptyList(),
    val average: Double = 0.0,
    val isPassing: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val gradeStatistics: Map<String, Any> = emptyMap(),
    val gradeTrend: List<Pair<Long, Double>> = emptyList()
)

class GradeViewModel : ViewModel() {
    private val repository = GradeRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(GradeUiState())
    val uiState: StateFlow<GradeUiState> = _uiState.asStateFlow()

    private val _grades = MutableStateFlow<List<Grade>>(emptyList())
    val grades: StateFlow<List<Grade>> = _grades.asStateFlow()

    init {
        loadUserSubjects()
    }

    private fun loadUserSubjects() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                Log.d("GradeViewModel", "Loading subjects for user: $userId")
                repository.getUserSubjects(userId).collect { subjects ->
                    Log.d("GradeViewModel", "Loaded ${subjects.size} subjects")
                    subjects.forEach { subject ->
                        Log.d("GradeViewModel", "Subject: id=${subject.id}, name=${subject.name}")
                    }
                    _uiState.value = _uiState.value.copy(
                        subjects = subjects,
                        isLoading = false
                    )
                    // If there's a selected subject, refresh its grades
                    _uiState.value.selectedSubject?.let { subject ->
                        refreshSubjectGrades(subject)
                    }
                }
            } catch (e: Exception) {
                Log.e("GradeViewModel", "Failed to load subjects", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load subjects: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun refreshSubjectGrades(subject: Subject) {
        viewModelScope.launch {
            try {
                repository.getGradesForSubject(subject.id).collect { grades ->
                    updateSubjectState(subject, grades)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load grades: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun updateSubjectState(subject: Subject, grades: List<Grade>) {
        val average = repository.calculateSubjectAverage(grades, subject)
        val isPassing = repository.isPassingGrade(average, subject)
        val statistics = repository.getGradeStatistics(grades)
        val trend = repository.getGradeTrend(grades)
        
        _uiState.value = _uiState.value.copy(
            grades = grades,
            average = average,
            isPassing = isPassing,
            isLoading = false,
            gradeStatistics = statistics,
            gradeTrend = trend
        )
    }

    fun selectSubjectById(subjectId: String) {
        Log.d("GradeViewModel", "Attempting to select subject with ID: $subjectId")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // First check if the subject is already in our list
                val existingSubject = _uiState.value.subjects.find { it.id == subjectId }
                if (existingSubject != null) {
                    Log.d("GradeViewModel", "Found existing subject: ${existingSubject.name}")
                    selectSubject(existingSubject)
                    return@launch
                }

                // If not found in the list, try to load it from repository
                Log.d("GradeViewModel", "Subject not found in list, loading from repository")
                val subject = repository.getSubjectById(subjectId)
                if (subject == null) {
                    Log.e("GradeViewModel", "Subject not found in repository with ID: $subjectId")
                    _uiState.value = _uiState.value.copy(
                        error = "Subject not found. Please try again.",
                        isLoading = false
                    )
                    return@launch
                }

                Log.d("GradeViewModel", "Found subject in repository: ${subject.name}")
                selectSubject(subject)
            } catch (e: Exception) {
                Log.e("GradeViewModel", "Error selecting subject", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to select subject: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun selectSubject(subject: Subject) {
        Log.d("GradeViewModel", "Selecting subject: ${subject.name} (${subject.id})")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    selectedSubject = subject,
                    isLoading = true,
                    error = null
                )
                refreshSubjectGrades(subject)
            } catch (e: Exception) {
                Log.e("GradeViewModel", "Error selecting subject", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to select subject: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun addGrade(
        score: Double,
        maxScore: Double,
        title: String,
        type: AssessmentType
    ) {
        Log.d("GradeViewModel", "Starting to add grade: score=$score, maxScore=$maxScore, title=$title, type=$type")
        
        val subject = _uiState.value.selectedSubject
        if (subject == null) {
            Log.e("GradeViewModel", "No subject selected")
            _uiState.value = _uiState.value.copy(
                error = "Please select a subject first"
            )
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GradeViewModel", "No user ID available")
            _uiState.value = _uiState.value.copy(
                error = "User not authenticated"
            )
            return
        }

        // Validate grade
        if (score < 0 || maxScore <= 0 || score > maxScore) {
            Log.e("GradeViewModel", "Invalid grade values: score=$score, maxScore=$maxScore")
            _uiState.value = _uiState.value.copy(
                error = "Invalid grade values. Score must be between 0 and max score."
            )
            return
        }

        val grade = Grade(
            userId = userId,
            subjectId = subject.id,
            score = score,
            maxScore = maxScore,
            title = title,
            type = type,
            date = Timestamp.now()
        )

        Log.d("GradeViewModel", "Created grade object: $grade")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                Log.d("GradeViewModel", "Calling repository.addGrade")
                repository.addGrade(grade)
                Log.d("GradeViewModel", "Grade added successfully")
                
                // Refresh grades for the current subject
                Log.d("GradeViewModel", "Refreshing grades for subject: ${subject.id}")
                refreshSubjectGrades(subject)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("GradeViewModel", "Error adding grade", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add grade: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun addSubject(name: String, passingGrade: Double) {
        val userId = auth.currentUser?.uid ?: return
        
        // Validate input
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Subject name cannot be empty"
            )
            return
        }

        if (passingGrade < 1.0 || passingGrade > 5.0) {
            _uiState.value = _uiState.value.copy(
                error = "Passing grade must be between 1.0 and 5.0"
            )
            return
        }

        val subject = Subject(
            userId = userId,
            name = name,
            passingGrade = passingGrade,
            weights = mapOf(
                AssessmentType.PRE_EXAM.name to 0.5,
                AssessmentType.ASSIGNMENT.name to 0.5
            )
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.addSubject(subject)
                // Select the newly added subject
                selectSubject(subject)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add subject: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.updateSubject(subject)
                // If this was the selected subject, update it
                if (_uiState.value.selectedSubject?.id == subject.id) {
                    selectSubject(subject)
                } else {
                    loadUserSubjects()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update subject: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.deleteSubject(subject)
                // If this was the selected subject, clear it
                if (_uiState.value.selectedSubject?.id == subject.id) {
                    _uiState.value = _uiState.value.copy(
                        selectedSubject = null,
                        grades = emptyList(),
                        average = 0.0,
                        isPassing = true,
                        gradeStatistics = emptyMap(),
                        gradeTrend = emptyList()
                    )
                }
                loadUserSubjects()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete subject: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getProgressByType(grades: List<Grade>, subject: Subject): Map<AssessmentType, Double> {
        return repository.getProgressByType(grades, subject)
    }

    fun calculateOverallProgress(grades: List<Grade>, subjects: List<Subject>): Map<AssessmentType, Double> {
        if (subjects.isEmpty()) return emptyMap()

        // Calculate progress for each subject
        val subjectProgresses = subjects.map { subject ->
            val subjectGrades = grades.filter { it.subjectId == subject.id }
            repository.getProgressByType(subjectGrades, subject)
        }

        // Combine progress across all subjects
        return AssessmentType.values().associateWith { type ->
            val typeProgresses = subjectProgresses.mapNotNull { it[type] }
            if (typeProgresses.isEmpty()) 0.0 else typeProgresses.average()
        }
    }

    fun loadGrades(subjectId: String) {
        viewModelScope.launch {
            repository.getGradesForSubject(subjectId).collect { grades ->
                _grades.value = grades
            }
        }
    }

    fun deleteGrade(subjectId: String, gradeId: String) {
        viewModelScope.launch {
            repository.deleteGrade(subjectId, gradeId)
            loadUserSubjects()
        }
    }
} 