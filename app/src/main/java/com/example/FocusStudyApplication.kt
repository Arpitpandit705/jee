package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.receiver.StudyAlarmReceiver
import com.example.service.FocusPeriodicSyncWorker
import com.example.service.HardcoreAppBlockerService
import com.example.util.StudyScheduler

class FocusStudyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initNotificationChannels()

        // Schedule daily morning alarms
        try {
            StudyScheduler.schedule7AmCheckIn(this)
            StudyScheduler.schedule8AmBlocker(this)
        } catch (e: Exception) {
            // Ignored in headless tests
        }

        // Enqueue 2-hour periodic sync worker
        try {
            FocusPeriodicSyncWorker.enqueuePeriodicSync(this)
        } catch (e: Exception) {
            // Ignored in headless tests
        }
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val focusChannel = NotificationChannel(
                HardcoreAppBlockerService.CHANNEL_ID,
                "Study Focus Lock",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Foreground notifications keeping focus lock active during lectures"
            }

            val reminderChannel = NotificationChannel(
                StudyAlarmReceiver.REMINDER_CHANNEL_ID,
                "Study Schedule Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily 7 AM check-in and 8 AM blocker auto-activation alerts"
            }

            val syncChannel = NotificationChannel(
                FocusPeriodicSyncWorker.CHANNEL_ID,
                "2-Hour Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic 2-hour reminders to complete pending lectures and DPPs"
            }

            manager.createNotificationChannels(listOf(focusChannel, reminderChannel, syncChannel))
        }
    }
}
