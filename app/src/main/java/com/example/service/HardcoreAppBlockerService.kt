package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HardcoreAppBlockerService : Service() {

    companion object {
        const val CHANNEL_ID = "study_focus_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_LOCK = "com.example.focusstudy.ACTION_START_LOCK"
        const val ACTION_STOP_LOCK = "com.example.focusstudy.ACTION_STOP_LOCK"
        const val ACTION_EMERGENCY_BREAK = "com.example.focusstudy.ACTION_EMERGENCY_BREAK"

        const val EXTRA_BLOCKED_APP_NAME = "blocked_app_name"

        var isServiceRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null
    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_LOCK -> {
                stopFocusLock()
                stopSelf()
            }
            ACTION_EMERGENCY_BREAK -> {
                handleEmergencyBreak()
            }
            ACTION_START_LOCK, null -> {
                startFocusLock()
            }
        }
        return START_STICKY
    }

    private fun startFocusLock() {
        isServiceRunning = true
        prefs.isHardcoreLockActive = true

        val notification = buildPersistentNotification(
            title = "Hardcore Study Mode Active",
            message = "Distractions are blocked. Focus on: ${prefs.activeLectureTitle.ifBlank { "Scheduled Lecture" }}"
        )
        startForeground(NOTIFICATION_ID, notification)

        startAppMonitorLoop()
    }

    private fun stopFocusLock() {
        isServiceRunning = false
        prefs.isHardcoreLockActive = false
        monitorJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleEmergencyBreak() {
        serviceScope.launch {
            // Pause blocker for 5 minutes
            monitorJob?.cancel()
            val breakNotif = buildPersistentNotification(
                title = "Emergency Break Active (5 Mins)",
                message = "Returning to strict lock soon. Use your break wisely!"
            )
            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.notify(NOTIFICATION_ID, breakNotif)

            delay(5 * 60 * 1000L) // 5 minutes

            // Re-engage strict lock
            startAppMonitorLoop()
        }
    }

    private fun startAppMonitorLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            while (isActive && prefs.isHardcoreLockActive) {
                if (usageStatsManager != null) {
                    val foregroundPackage = getForegroundPackage(usageStatsManager)
                    if (foregroundPackage != null && foregroundPackage != packageName) {
                        val blockedList = prefs.blockedPackages
                        if (blockedList.contains(foregroundPackage)) {
                            prefs.incrementBlockedCount()
                            interceptDistraction(foregroundPackage)
                        }
                    }
                }
                delay(1200L)
            }
        }
    }

    private fun getForegroundPackage(manager: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            now - 5000,
            now
        ) ?: return null

        var recentPackage: String? = null
        var lastUsedTime = 0L
        for (usage in stats) {
            if (usage.lastTimeUsed > lastUsedTime) {
                lastUsedTime = usage.lastTimeUsed
                recentPackage = usage.packageName
            }
        }
        return recentPackage
    }

    private fun interceptDistraction(blockedPkg: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BLOCKED_APP_NAME, blockedPkg)
            putExtra("SHOW_LOCK_OVERLAY", true)
        }
        startActivity(intent)
    }

    private fun buildPersistentNotification(title: String, message: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Focus Lock",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Foreground notifications keeping focus lock active during lectures"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        monitorJob?.cancel()
    }
}
