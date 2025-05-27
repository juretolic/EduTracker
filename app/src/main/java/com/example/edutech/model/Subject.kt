package com.example.edutech.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class AssessmentType {
    PRE_EXAM,
    ASSIGNMENT
}

data class Subject(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val passingGrade: Double = 0.0,
    val weights: Map<String, Double> = mapOf(
        AssessmentType.PRE_EXAM.name to 0.5,
        AssessmentType.ASSIGNMENT.name to 0.5
    ),
    val createdAt: Timestamp = Timestamp.now()
) 