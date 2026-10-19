package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.Course
import com.example.model.DailyPlan
import com.example.model.Lecture
import com.example.model.SyncLog
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class FirestoreRepository(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentUserIdOrNull(): String? = auth.currentUser?.uid

    // User Profile
    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot -> snapshot.toObject(UserProfile::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val path = "users/$uid"
        try {
            db.collection("users").document(uid).set(profile.copy(userId = uid)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // Courses
    fun observeCourses(): Flow<List<Course>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/courses"
        emitAll(
            db.collection("users").document(uid).collection("courses")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Course::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addCourse(course: Course): String {
        val uid = requireUserId()
        val docId = if (course.id.isNotBlank()) course.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("courses").document(docId)
        try {
            val payload = course.copy(id = docId, userId = uid)
            docRef.set(payload).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCourse(course: Course) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("courses").document(course.id)
        try {
            docRef.set(course.copy(userId = uid)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // Lectures
    fun observeLectures(courseId: String): Flow<List<Lecture>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/courses/$courseId/lectures"
        emitAll(
            db.collection("users").document(uid).collection("courses").document(courseId).collection("lectures")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Lecture::class.java).sortedBy { it.lectureNumber } }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addLecture(lecture: Lecture): String {
        val uid = requireUserId()
        val docId = if (lecture.id.isNotBlank()) lecture.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid)
            .collection("courses").document(lecture.courseId)
            .collection("lectures").document(docId)
        try {
            val payload = lecture.copy(id = docId, userId = uid)
            docRef.set(payload).await()
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateLecture(lecture: Lecture) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("courses").document(lecture.courseId)
            .collection("lectures").document(lecture.id)
        try {
            docRef.set(lecture.copy(userId = uid)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // Daily Plans
    fun getTodayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun observeTodayPlan(): Flow<DailyPlan?> = flow {
        val uid = requireUserId()
        val today = getTodayDateKey()
        val docRef = db.collection("users").document(uid).collection("daily_plans").document(today)
        emitAll(
            docRef.snapshots()
                .map { snapshot -> snapshot.toObject(DailyPlan::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun saveDailyPlan(plan: DailyPlan) {
        val uid = requireUserId()
        val docId = if (plan.id.isNotBlank()) plan.id else getTodayDateKey()
        val docRef = db.collection("users").document(uid).collection("daily_plans").document(docId)
        try {
            docRef.set(plan.copy(id = docId, userId = uid, date = docId)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // 2-Hour Sync Telemetry Logs
    suspend fun logSync(studyMinutes: Long, blockedAttempts: Long, deviceState: String = "ACTIVE") {
        val uid = currentUserIdOrNull() ?: return
        val docId = UUID.randomUUID().toString()
        val docRef = db.collection("users").document(uid).collection("sync_logs").document(docId)
        try {
            val log = SyncLog(
                id = docId,
                userId = uid,
                studyMinutesLogged = studyMinutes,
                blockedAppAttempts = blockedAttempts,
                deviceState = deviceState
            )
            docRef.set(log).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }
}
