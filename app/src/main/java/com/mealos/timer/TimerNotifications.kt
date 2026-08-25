package com.mealos.timer

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mealos.MainActivity
import com.mealos.R

object TimerNotifications {
    const val channelId = "mealos_cooking_timers"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.timer_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.timer_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, id: String, label: String, triggerAt: Long) {
        val intent = Intent(context, TimerReceiver::class.java).apply {
            putExtra("timer_id", id)
            putExtra("timer_label", label)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context, id: String) {
        val pending = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent(context, TimerReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
    }

    fun canPostAlerts(context: Context): Boolean {
        val runtimePermissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!runtimePermissionGranted || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(channelId)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun notificationSettingsIntent(context: Context): Intent {
        val manager = context.getSystemService(NotificationManager::class.java)
        return if (manager.getNotificationChannel(channelId) != null) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
    }

    fun showFinished(context: Context, id: String, label: String) {
        if (!canPostAlerts(context)) return
        val contentIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$label is ready")
            .setContentText("Open MealOS to continue cooking.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
    }

    fun reschedulePersisted(context: Context) {
        val store = TimerStore(context)
        val timers = store.load().toMutableMap()
        val now = System.currentTimeMillis()
        timers.values.toList().forEach { timer ->
            val endAt = timer.endAtEpochMs
            if (!timer.running || endAt == null) return@forEach
            if (endAt > now) {
                schedule(context, timer.id, timer.label, endAt)
            } else {
                timers[timer.id] = timer.copy(running = false, endAtEpochMs = null, remainingMs = 0)
                showFinished(context, timer.id, timer.label)
            }
        }
        store.save(timers)
    }
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("timer_id") ?: return
        val label = intent.getStringExtra("timer_label") ?: "Cooking timer"
        TimerStore(context).update(id) { timer ->
            timer.copy(running = false, endAtEpochMs = null, remainingMs = 0)
        }
        TimerNotifications.showFinished(context, id, label)
    }
}

class TimerRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ->
                TimerNotifications.reschedulePersisted(context)
        }
    }
}
