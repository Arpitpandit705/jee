package com.example.data

import com.example.model.Course
import com.example.model.Lecture
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Seeds the user's real PW syllabus (from the bundled plan.json) into Firestore.
 *
 * Write-burst protection: Firestore writes are flushed in chunks of at most
 * [BATCH_LIMIT] operations with a pause between chunks, so first-run import
 * never slams the quota at once. User-scoped paths mirror what firestore.rules
 * allow (only /users/{uid}/** with matching userId fields).
 */
class PwPlanImporter(
    private val db: FirebaseFirestore,
    private val uid: String
) {

    suspend fun importAll(plan: PwStudyPlan, existingUserProfile: UserProfile? = null) {
        var importedMinutes = 0L
        var completedCount = 0L

        for (coursePlan in plan.courses) {
            val plannedTotal = if (coursePlan.fullyComplete) {
                // Physics 11th basics: completed outside snapshot, chapter-level only
                70L
            } else {
                coursePlan.chapters.sumOf { it.lectures.size.toLong() }
            }
            val completed = if (coursePlan.fullyComplete) 70L
            else coursePlan.chapters.sumOf { ch -> ch.lectures.count { it.isCompleted }.toLong() }

            val courseRef = db.collection("users").document(uid)
                .collection("courses").document(UUID.randomUUID().toString())

            courseRef.set(
                Course(
                    id = courseRef.id,
                    userId = uid,
                    title = coursePlan.key,
                    subject = PwStudyPlan.run { coursePlan.subjectTitle() },
                    totalLectures = plannedTotal,
                    completedLectures = completed,
                    hasDppDefault = coursePlan.chapters.any { it.dpps.isNotEmpty() }
                )
            ).await()

            if (coursePlan.fullyComplete) continue

            var pending = 0
            var batch = db.batch()
            var lectureNumber = 0L

            for (chapter in coursePlan.chapters) {
                val dppUrl = chapter.dpps.firstOrNull()?.url.orEmpty()
                val dppName = chapter.dpps.firstOrNull()?.name.orEmpty()
                for (lec in chapter.lectures) {
                    lectureNumber += 1
                    val docRef = courseRef.collection("lectures")
                        .document(UUID.randomUUID().toString())
                    batch.set(
                        docRef,
                        Lecture(
                            id = docRef.id,
                            userId = uid,
                            courseId = courseRef.id,
                            lectureNumber = lectureNumber,
                            title = lec.title,
                            durationMinutes = lec.durationMin.coerceAtLeast(1L),
                            hasDpp = dppUrl.isNotBlank(),
                            dppSolved = lec.isCompleted,
                            isCompleted = lec.isCompleted,
                            isVerified = lec.isCompleted,
                            verificationType = if (lec.isCompleted) "PW_IMPORTED" else "",
                            revisionDueDays = 3L,
                            pwId = lec.pwId,
                            sourceTag = "PW:${coursePlan.key}",
                            dppPdfUrl = dppUrl,
                            dppPdfName = dppName
                        )
                    )
                    pending += 1
                    if (lec.isCompleted) completedCount += 1
                    importedMinutes += lec.durationMin.coerceAtLeast(1L)

                    if (pending >= BATCH_LIMIT) {
                        batch.commit().await()
                        delay(BATCH_PAUSE_MS)
                        pending = 0
                        batch = db.batch()
                    }
                }
            }
            if (pending > 0) {
                batch.commit().await()
                delay(BATCH_PAUSE_MS)
            }
        }

        // Reflect imported progress on the user profile (merge, never wipe)
        db.collection("users").document(uid).set(
            (existingUserProfile ?: UserProfile()).copy(
                userId = uid,
                completedLecturesCount = existingUserProfile?.completedLecturesCount ?: completedCount,
                totalStudyMinutes = (existingUserProfile?.totalStudyMinutes ?: 0L).coerceAtLeast(importedMinutes)
            ),
            SetOptions.merge()
        ).await()
    }

    companion object {
        private const val BATCH_LIMIT = 400        // Firestore batch max = 500 ops
        private const val BATCH_PAUSE_MS = 750L    // write-burst protection
    }
}
