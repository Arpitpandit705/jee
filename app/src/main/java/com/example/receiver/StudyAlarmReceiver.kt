package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import com.example.service.HardcoreAppBlockerService
import com.example.util.StudyScheduler

class StudyAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val REMINDER_CHANNEL_ID = "study_reminders_channel"
        const val NOTIF_ID_7AM = 7001
        const val NOTIF_ID_8AM = 8001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager(context)

        when (intent.action) {
            StudyScheduler.ACTION_7AM_PLANNER -> {
                show7AmPlannerNotification(context)
                // Schedule next 7 AM alarm
                StudyScheduler.schedule7AmCheckIn(context)
            }
            StudyScheduler.ACTION_8AM_BLOCKER -> {
                if (prefs.is8AmBlockerEnabled) {
                    show8AmLockdownNotification(context)
                    // Auto-start blocker service if core permissions are granted
                    val serviceIntent = Intent(context, HardcoreAppBlockerService::class.java).apply {
                        action = HardcoreAppBlockerService.ACTION_START_LOCK
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
                // Schedule next 8 AM alarm
                StudyScheduler.schedule8AmBlocker(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule morning alarms on device reboot
                StudyScheduler.schedule7AmCheckIn(context)
                StudyScheduler.schedule8AmBlocker(context)
            }
        }
    }

    private fun show7AmPlannerNotification(context: Context) {
        createReminderChannel(context)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TAB", "PLANNER")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            700,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("7:00 AM Study Check-In")
            .setContentText("What lectures are you completing today? Set your schedule and DPP targets now!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_7AM, notification)
    }

    private fun show8AmLockdownNotification(context: Context) {
        createReminderChannel(context)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TAB", "FOCUS_LOCK")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            800,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("8:00 AM Strict Focus Lock Activated")
            .setContentText("Distracting apps are now blocked. Time to start your scheduled lectures!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_8AM, notification)
    }

    private fun createReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Study Schedule Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily 7 AM check-in and 8 AM blocker auto-activation alerts"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
