package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("focus_study_prefs", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_BLOCKED_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.snapchat.android",
            "com.twitter.android",
            "com.reddit.frontpage",
            "com.facebook.katana",
            "tv.twitch.android.app",
            "com.netflix.mediaclient",
            "com.discord",
            "com.pinterest",
            "com.whatsapp"
        )
    }

    var isPermissionOnboardingCompleted: Boolean
        get() = prefs.getBoolean("perm_onboarded", false)
        set(value) = prefs.edit().putBoolean("perm_onboarded", value).apply()

    var is8AmBlockerEnabled: Boolean
        get() = prefs.getBoolean("blocker_8am_enabled", true)
        set(value) = prefs.edit().putBoolean("blocker_8am_enabled", value).apply()

    var isHardcoreLockActive: Boolean
        get() = prefs.getBoolean("hardcore_lock_active", false)
        set(value) = prefs.edit().putBoolean("hardcore_lock_active", value).apply()

    var activeCourseId: String
        get() = prefs.getString("active_course_id", "") ?: ""
        set(value) = prefs.edit().putString("active_course_id", value).apply()

    var activeLectureId: String
        get() = prefs.getString("active_lecture_id", "") ?: ""
        set(value) = prefs.edit().putString("active_lecture_id", value).apply()

    var activeLectureTitle: String
        get() = prefs.getString("active_lecture_title", "") ?: ""
        set(value) = prefs.edit().putString("active_lecture_title", value).apply()

    var activeFocusTotalSeconds: Long
        get() = prefs.getLong("active_focus_total_sec", 3600L)
        set(value) = prefs.edit().putLong("active_focus_total_sec", value).apply()

    var activeFocusRemainingSeconds: Long
        get() = prefs.getLong("active_focus_remain_sec", 3600L)
        set(value) = prefs.edit().putLong("active_focus_remain_sec", value).apply()

    var emergencyBreaksRemaining: Int
        get() = prefs.getInt("emergency_breaks_left", 1)
        set(value) = prefs.edit().putInt("emergency_breaks_left", value).apply()

    var distractionsBlockedCount: Long
        get() = prefs.getLong("distractions_blocked_count", 0L)
        set(value) = prefs.edit().putLong("distractions_blocked_count", value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong("last_sync_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_sync_timestamp", value).apply()

    var blockedPackages: Set<String>
        get() = prefs.getStringSet("blocked_packages", DEFAULT_BLOCKED_PACKAGES) ?: DEFAULT_BLOCKED_PACKAGES
        set(value) = prefs.edit().putStringSet("blocked_packages", value).apply()

    fun is7AmCheckInDoneToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return prefs.getString("last_7am_checkin_date", "") == today
    }

    fun mark7AmCheckInDone() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_7am_checkin_date", today).apply()
    }

    fun incrementBlockedCount() {
        distractionsBlockedCount += 1
    }

    var isStudyOnboardingCompleted: Boolean
        get() = prefs.getBoolean("study_onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("study_onboarding_completed", value).apply()

    var dailyLectureQuota: Int
        get() = prefs.getInt("daily_lecture_quota", 3)
        set(value) = prefs.edit().putInt("daily_lecture_quota", value).apply()

    var dailyStudyHoursTarget: Int
        get() = prefs.getInt("daily_study_hours_target", 4)
        set(value) = prefs.edit().putInt("daily_study_hours_target", value).apply()

    var dppTrackingEnabled: Boolean
        get() = prefs.getBoolean("dpp_tracking_enabled", true)
        set(value) = prefs.edit().putBoolean("dpp_tracking_enabled", value).apply()

    var dailyDppQuota: Int
        get() = prefs.getInt("daily_dpp_quota", 15)
        set(value) = prefs.edit().putInt("daily_dpp_quota", value).apply()

    var checkInTime7Am: String
        get() = prefs.getString("checkin_time_7am", "07:00 AM") ?: "07:00 AM"
        set(value) = prefs.edit().putString("checkin_time_7am", value).apply()

    var blockerTime8Am: String
        get() = prefs.getString("blocker_time_8am", "08:00 AM") ?: "08:00 AM"
        set(value) = prefs.edit().putString("blocker_time_8am", value).apply()

    var batchApiKey: String
        get() = prefs.getString("batch_api_key", "") ?: ""
        set(value) = prefs.edit().putString("batch_api_key", value).apply()

    var batchPortalUrl: String
        get() = prefs.getString("batch_portal_url", "https://youtube.com") ?: "https://youtube.com"
        set(value) = prefs.edit().putString("batch_portal_url", value).apply()

    var batchStartLecture: Long
        get() = prefs.getLong("batch_start_lecture", 1L)
        set(value) = prefs.edit().putLong("batch_start_lecture", value).apply()

    var batchCourseName: String
        get() = prefs.getString("batch_course_name", "Target Batch 2026") ?: "Target Batch 2026"
        set(value) = prefs.edit().putString("batch_course_name", value).apply()
}
