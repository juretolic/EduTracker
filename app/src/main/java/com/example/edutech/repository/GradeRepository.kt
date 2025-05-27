package com.example.edutech.repository

import android.util.Log
import com.example.edutech.model.AssessmentType
import com.example.edutech.model.Grade
import com.example.edutech.model.Subject
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.ListenerRegistration

class GradeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val subjectsCollection = firestore.collection("subjects")
    private val gradesCollection = firestore.collection("grades")

    fun getUserSubjects(userId: String): Flow<List<Subject>> = flow {
        try {
            Log.d("GradeRepository", "Getting subjects for user: $userId")
            val snapshot = subjectsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val subjects = snapshot.toObjects(Subject::class.java)
            Log.d("GradeRepository", "Found ${subjects.size} subjects")
            subjects.forEach { subject ->
                Log.d("GradeRepository", "Subject: id=${subject.id}, name=${subject.name}")
            }
            
            emit(subjects)
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error getting subjects", e)
            emit(emptyList())
        }
    }

    fun getGradesForSubject(subjectId: String): Flow<List<Grade>> = callbackFlow {
        var registration: ListenerRegistration? = null
        
        try {
            Log.d("GradeRepository", "Setting up grades listener for subject: $subjectId")
            
            val query = gradesCollection
                .whereEqualTo("subjectId", subjectId)
                .orderBy("date", Query.Direction.DESCENDING)

            registration = query.addSnapshotListener { snapshot, error ->
                when {
                    error != null -> {
                        if (error is FirebaseFirestoreException && 
                            error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            // Index is not ready yet
                            val indexUrl = error.message?.substringAfter("You can create it here: ")
                            Log.w("GradeRepository", "Index not ready. Please create the index at: $indexUrl")
                            trySend(emptyList<Grade>())
                            throw Exception("The database is still setting up. Your grade has been saved but may take a few minutes to appear in the list.")
                        } else {
                            Log.e("GradeRepository", "Error getting grades", error)
                            trySend(emptyList<Grade>())
                        }
                    }
                    snapshot != null -> {
                        val grades = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Grade::class.java)
                            } catch (e: Exception) {
                                Log.e("GradeRepository", "Error converting document to Grade", e)
                                null
                            }
                        }
                        Log.d("GradeRepository", "Received ${grades.size} grades for subject $subjectId")
                        trySend(grades)
                    }
                    else -> {
                        Log.d("GradeRepository", "No grades found for subject $subjectId")
                        trySend(emptyList<Grade>())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error setting up grades listener", e)
            trySend(emptyList<Grade>())
        }

        awaitClose {
            try {
                registration?.remove()
                Log.d("GradeRepository", "Removed grades listener for subject: $subjectId")
            } catch (e: Exception) {
                Log.e("GradeRepository", "Error removing listener", e)
            }
        }
    }.catch { e ->
        Log.e("GradeRepository", "Flow error", e)
        emit(emptyList<Grade>())
    }

    suspend fun addSubject(subject: Subject) {
        try {
            subjectsCollection.add(subject).await()
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error adding subject", e)
            throw e
        }
    }

    private suspend fun ensureGradesIndexExists() {
        try {
            Log.d("GradeRepository", "Ensuring grades index exists")
            
            // Create a simple query to trigger index creation
            val query = gradesCollection
                .whereEqualTo("subjectId", "dummy")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(1)
            
            // Execute the query to trigger index creation
            query.get().await()
            Log.d("GradeRepository", "Grades index creation triggered")
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException && 
                e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                // Index is not ready, but it will be created automatically
                Log.d("GradeRepository", "Index creation in progress")
            } else {
                Log.e("GradeRepository", "Error triggering index creation", e)
            }
            // Don't throw the error - we'll still try to add the grade
        }
    }

    suspend fun addGrade(grade: Grade) {
        try {
            Log.d("GradeRepository", "Starting to add grade: $grade")
            
            // Ensure index exists before adding grade
            ensureGradesIndexExists()
            
            // Calculate grade fields before saving
            val gradeValue = grade.getGrade()
            val isValid = grade.isValid()
            val gradeDescription = grade.getGradeDescription()
            
            val gradeWithCalculatedFields = grade.copy(
                isValidGrade = isValid,
                gradePoint = gradeValue,
                gradeValue = gradeValue,
                description = gradeDescription
            )
            
            Log.d("GradeRepository", "Grade with calculated fields: $gradeWithCalculatedFields")
            
            // Add the document and get the reference
            val docRef = gradesCollection.add(gradeWithCalculatedFields).await()
            Log.d("GradeRepository", "Document created with ID: ${docRef.id}")
            
            // Update the document with its ID
            val gradeWithId = gradeWithCalculatedFields.copy(id = docRef.id)
            docRef.set(gradeWithId).await()
            
            Log.d("GradeRepository", "Successfully added grade with ID: ${docRef.id}")
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error adding grade: ${e.message}", e)
            throw e
        }
    }

    fun calculateSubjectAverage(grades: List<Grade>, subject: Subject): Double {
        if (grades.isEmpty()) {
            Log.d("GradeRepository", "No grades found for subject")
            return 0.0
        }

        // Filter valid grades
        val validGrades = grades.filter { it.isValid() }
        if (validGrades.isEmpty()) {
            Log.d("GradeRepository", "No valid grades found")
            return 0.0
        }

        // Calculate progress by type
        val progressByType = getProgressByType(validGrades, subject)
        
        // Sum up the weighted averages
        val totalProgress = progressByType.values.sum()
        
        // Get total weight
        val totalWeight = subject.weights.values.sum()
        
        // Calculate final average
        val finalAverage = if (totalWeight > 0) totalProgress / totalWeight else 0.0
        
        Log.d("GradeRepository", "Progress by type: $progressByType")
        Log.d("GradeRepository", "Total progress: $totalProgress")
        Log.d("GradeRepository", "Total weight: $totalWeight")
        Log.d("GradeRepository", "Final average: $finalAverage")
        
        return finalAverage
    }

    fun getGradeStatistics(grades: List<Grade>): Map<String, Any> {
        if (grades.isEmpty()) {
            return mapOf(
                "average" to 0.0,
                "highest" to 0.0,
                "lowest" to 0.0,
                "gradeDistribution" to emptyMap<String, Int>()
            )
        }

        val validGrades = grades.filter { it.isValid() }
        if (validGrades.isEmpty()) {
            return mapOf(
                "average" to 0.0,
                "highest" to 0.0,
                "lowest" to 0.0,
                "gradeDistribution" to emptyMap<String, Int>()
            )
        }

        val gradeValues = validGrades.map { it.getGrade() }
        val average = gradeValues.average()
        val highest = gradeValues.maxOrNull() ?: 0.0
        val lowest = gradeValues.minOrNull() ?: 0.0

        // Calculate grade distribution
        val distribution = gradeValues.groupBy { grade ->
            when {
                grade >= 4.5 -> "5"
                grade >= 3.5 -> "4"
                grade >= 2.5 -> "3"
                grade >= 1.5 -> "2"
                else -> "1"
            }
        }.mapValues { it.value.size }

        return mapOf(
            "average" to average,
            "highest" to highest,
            "lowest" to lowest,
            "gradeDistribution" to distribution
        )
    }

    fun getGradeTrend(grades: List<Grade>): List<Pair<Long, Double>> {
        return grades
            .filter { it.isValid() }
            .sortedBy { it.date }
            .map { grade ->
                Pair(grade.date.seconds, grade.getGrade())
            }
    }

    fun isPassingGrade(average: Double, subject: Subject): Boolean {
        return average >= subject.passingGrade
    }

    fun getProgressByType(grades: List<Grade>, subject: Subject): Map<AssessmentType, Double> {
        // Filter valid grades first
        val validGrades = grades.filter { it.isValid() }
        
        // Group grades by type and calculate average for each type
        return AssessmentType.values().associateWith { type ->
            val typeGrades = validGrades.filter { it.type == type }
            if (typeGrades.isEmpty()) {
                0.0
            } else {
                // Calculate weighted average for this type
                val typeAverage = typeGrades.map { it.getGrade() }.average()
                // Apply the subject's weight for this type
                val weight = subject.weights[type.name] ?: 0.0
                typeAverage * weight
            }
        }
    }

    suspend fun updateSubject(subject: Subject) {
        try {
            subjectsCollection.document(subject.id).set(subject).await()
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error updating subject", e)
            throw e
        }
    }

    suspend fun deleteSubject(subject: Subject) {
        try {
            // Delete all grades for this subject first
            val grades = gradesCollection
                .whereEqualTo("subjectId", subject.id)
                .get()
                .await()
            
            grades.forEach { grade ->
                gradesCollection.document(grade.id).delete().await()
            }
            
            // Then delete the subject
            subjectsCollection.document(subject.id).delete().await()
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error deleting subject", e)
            throw e
        }
    }

    suspend fun deleteGrade(subjectId: String, gradeId: String) {
        try {
            // Verify the grade belongs to the subject before deleting
            val grade = gradesCollection
                .document(gradeId)
                .get()
                .await()
                .toObject(Grade::class.java)

            if (grade?.subjectId == subjectId) {
                gradesCollection.document(gradeId).delete().await()
            } else {
                Log.e("GradeRepository", "Grade $gradeId does not belong to subject $subjectId")
                throw IllegalStateException("Grade does not belong to the specified subject")
            }
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error deleting grade", e)
            throw e
        }
    }

    suspend fun getSubjectById(subjectId: String): Subject? {
        return try {
            val doc = subjectsCollection.document(subjectId).get().await()
            doc.toObject(Subject::class.java)
        } catch (e: Exception) {
            Log.e("GradeRepository", "Error getting subject by ID: $subjectId", e)
            null
        }
    }
} 