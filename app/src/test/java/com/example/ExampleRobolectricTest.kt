package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.model.Course
import com.example.model.DailyPlan
import com.example.model.Lecture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FocusStudy", appName)
    }

    @Test
    fun `preferences manager initializes with default blocked packages`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        assertTrue(prefs.blockedPackages.contains("com.google.android.youtube"))
        assertTrue(prefs.blockedPackages.contains("com.instagram.android"))
        assertTrue(prefs.is8AmBlockerEnabled)
    }

    @Test
    fun `course and lecture models initialize correctly`() {
        val course = Course(
            id = "c1",
            title = "Modern Physics",
            subject = "Physics",
            totalLectures = 40L,
            hasDppDefault = true
        )
        assertEquals("Modern Physics", course.title)
        assertEquals(40L, course.totalLectures)
        assertTrue(course.hasDppDefault)

        val lecture = Lecture(
            id = "l1",
            courseId = "c1",
            lectureNumber = 1L,
            title = "Photoelectric Effect",
            durationMinutes = 60L,
            hasDpp = true,
            dppSolved = false,
            isCompleted = false,
            isVerified = false
        )
        assertEquals(1L, lecture.lectureNumber)
        assertFalse(lecture.dppSolved)
        assertFalse(lecture.isVerified)
    }

    @Test
    fun `daily plan calculates planned minutes`() {
        val plan = DailyPlan(
            id = "2026-10-18",
            userId = "user123",
            plannedLectureIds = listOf("l1", "l2"),
            totalPlannedMinutes = 120L
        )
        assertEquals(2, plan.plannedLectureIds.size)
        assertEquals(120L, plan.totalPlannedMinutes)
    }

    @Test
    fun `speed advisor recommends dynamic playback speed accurately`() {
        val derivationRec = com.example.util.SpeedAdvisor.getRecommendation(
            "Rotational Dynamics Derivation",
            "Physics"
        )
        assertEquals(1.25f, derivationRec.speed, 0.01f)

        val dppRec = com.example.util.SpeedAdvisor.getRecommendation(
            "DPP 4 Problem Practice",
            "Mathematics"
        )
        assertEquals(1.75f, dppRec.speed, 0.01f)

        val standardRec = com.example.util.SpeedAdvisor.getRecommendation(
            "Introduction to Chemical Bonding",
            "Chemistry"
        )
        assertEquals(1.50f, standardRec.speed, 0.01f)
    }

    @Test
    fun `onboarding preferences store batch portal settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        prefs.batchApiKey = "test_key_123"
        prefs.batchStartLecture = 5L
        prefs.dailyLectureQuota = 4

        assertEquals("test_key_123", prefs.batchApiKey)
        assertEquals(5L, prefs.batchStartLecture)
        assertEquals(4, prefs.dailyLectureQuota)
    }
}
