package com.example.edutech.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlin.math.roundToInt

data class Grade(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val score: Double = 0.0,
    val maxScore: Double = 0.0,
    val title: String = "",
    val type: AssessmentType = AssessmentType.ASSIGNMENT,
    val date: Timestamp = Timestamp.now(),
    val gradePoint: Double = 0.0,
    val isValidGrade: Boolean = true,
    val gradeValue: Double = 0.0,
    val description: String = ""
) {
    fun isValid(): Boolean {
        return score >= 0 && maxScore > 0 && score <= maxScore
    }

    fun getGrade(): Double {
        if (!isValid()) return 0.0
        
        // Convert score to percentage
        val percentage = (score / maxScore) * 100
        
        // Convert percentage to 1-5 scale
        return when {
            percentage >= 90 -> 5.0
            percentage >= 80 -> 4.5
            percentage >= 70 -> 4.0
            percentage >= 60 -> 3.5
            percentage >= 50 -> 3.0
            percentage >= 40 -> 2.5
            percentage >= 30 -> 2.0
            percentage >= 20 -> 1.5
            percentage >= 10 -> 1.0
            else -> 0.0
        }
    }

    fun getGradeDescription(): String {
        val grade = getGrade()
        return when {
            grade >= 4.5 -> "Excellent"
            grade >= 3.5 -> "Very Good"
            grade >= 2.5 -> "Good"
            grade >= 1.5 -> "Satisfactory"
            grade >= 1.0 -> "Passing"
            else -> "Failing"
        }
    }
} 