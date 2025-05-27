package com.example.edutech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edutech.model.Subject
import com.example.edutech.repository.SubjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubjectViewModel : ViewModel() {
    private val subjectRepository = SubjectRepository()
    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            subjectRepository.getSubjects().collect { subjects ->
                _subjects.value = subjects
            }
        }
    }

    fun addSubject(name: String, passingGrade: Double) {
        viewModelScope.launch {
            subjectRepository.addSubject(name, passingGrade)
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(subjectId)
        }
    }
} 