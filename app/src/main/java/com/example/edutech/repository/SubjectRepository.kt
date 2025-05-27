package com.example.edutech.repository

import android.util.Log
import com.example.edutech.model.Subject
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SubjectRepository {
    private val db = FirebaseFirestore.getInstance()
    private val subjectsCollection = db.collection("subjects")

    fun getSubjects(): Flow<List<Subject>> = callbackFlow {
        val subscription = subjectsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SubjectRepository", "Error getting subjects", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val subjects = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Subject::class.java)?.copy(id = doc.id)
                    }
                    trySend(subjects)
                }
            }

        awaitClose { subscription.remove() }
    }

    suspend fun addSubject(name: String, passingGrade: Double): Result<Subject> {
        return try {
            val subject = Subject(
                name = name,
                passingGrade = passingGrade
            )
            val docRef = subjectsCollection.add(subject).await()
            Result.success(subject.copy(id = docRef.id))
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Error adding subject", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        return try {
            subjectsCollection.document(subjectId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Error deleting subject", e)
            Result.failure(e)
        }
    }
} 