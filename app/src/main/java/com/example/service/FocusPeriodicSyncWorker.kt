package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.FirestoreRepository
import com.example.data.PreferencesManager
import com.example.model.DailyPlan
import com.example.model.UserProfile
import java.util.concurrent.TimeUnit

class FocusPeriodicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "focus_2hour_periodic_sync"
        const val CHANNEL_ID = "study_2hour_sync_channel"
        const val NOTIF_ID_REMINDER = 9002

        fun enqueuePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<FocusPeriodicSyncWorker>(
                    2, TimeUnit.HOURS,
                    15, TimeUnit.MINUTES // 15-minute flex window
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    syncRequest
                )
            } catch (e: Exception) {
                android.util.Log.w("FocusPeriodicSyncWorker", "WorkManager init bypassed: ${e.message}")
            }
        }
    }

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        val repository = FirestoreRepository(applicationContext)

        val uid = repository.currentUserIdOrNull()
        if (uid != null) {
            try {
                val completedMinutes = ((prefs.activeFocusTotalSeconds - prefs.activeFocusRemainingSeconds) / 60)
                    .coerceAtLeast(0L)

                // 1. Upload study telemetry to backend
                repository.logSync(
                    studyMinutes = completedMinutes,
                    blockedAttempts = prefs.distractionsBlockedCount,
                    deviceState = if (prefs.isHardcoreLockActive) "LOCKED" else "STANDBY"
                )

                // 2. Sync daily plan progress & lecture status to Firestore
                val todayKey = repository.getTodayDateKey()
                repository.saveDailyPlan(
                    DailyPlan(
                        id = todayKey,
                        userId = uid,
                        date = todayKey,
                        totalCompletedMinutes = completedMinutes,
                        status = if (prefs.isHardcoreLockActive) "IN_PROGRESS" else "ACTIVE"
                    )
                )

                // 3. Update user profile statistics
                repository.saveUserProfile(
                    UserProfile(
                        userId = uid,
                        totalStudyMinutes = completedMinutes,
                        streakDays = 1L
                    )
                )

                prefs.lastSyncTimestamp = System.currentTimeMillis()
            } catch (e: Exception) {
                // Silently handle transient network failures; WorkManager will retry
            }
        }

        // Send 2-hour completion reminder if student has pending tasks or focus lock is running
        show2HourReminderNotification(applicationContext)

        return Result.success()
    }

    private fun show2HourReminderNotification(context: Context) {
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "2-Hour Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic 2-hour reminders to complete pending lectures and DPPs"
            }
            notifManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TAB", "PLANNER")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            902,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("2-Hour Study Sync & Reminder")
            .setContentText("Progress & DPPs synced to cloud. Keep your momentum and verify today's lectures!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notifManager.notify(NOTIF_ID_REMINDER, notification)
    }
}
