package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val totalStudyMinutes: Long = 0L,
    val streakDays: Long = 1L,
    val completedLecturesCount: Long = 0L,
    val completedDppsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val totalLectures: Long = 30L,
    val completedLectures: Long = 0L,
    val hasDppDefault: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Lecture(
    @DocumentId val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val lectureNumber: Long = 1L,
    val title: String = "",
    val durationMinutes: Long = 60L,
    val hasDpp: Boolean = true,
    val dppSolved: Boolean = false,
    val isCompleted: Boolean = false,
    val isVerified: Boolean = false,
    val verificationType: String = "", // "NOTES_PHOTO", "QUIZ", "ACTIVE_RECALL"
    val verificationScore: Long = 0L,
    val revisionDueDays: Long = 3L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyPlan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "", // YYYY-MM-DD
    val totalPlannedMinutes: Long = 0L,
    val totalCompletedMinutes: Long = 0L,
    val plannedLectureIds: List<String> = emptyList(),
    val status: String = "PENDING", // PENDING, ACTIVE, COMPLETED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SyncLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studyMinutesLogged: Long = 0L,
    val blockedAppAttempts: Long = 0L,
    val deviceState: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BlockedAppInfo(
    val packageName: String,
    val appName: String,
    val iconName: String,
    val isBlocked: Boolean = true
)

data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = ""
)

data class VerificationResult(
    val isVerified: Boolean = false,
    val score: Int = 0,
    val feedback: String = "",
    val identifiedConcepts: List<String> = emptyList()
)
