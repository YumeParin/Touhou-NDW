package com.example.touhoundw

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import java.util.Calendar

class TouhouWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Schedule the first update when the first widget is placed
        scheduleNextUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel the alarm when the last widget is removed
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getUpdatePendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Let the AppWidgetProvider handle its own intents first.
        super.onReceive(context, intent)

        val action = intent.action
        if (ACTION_AUTO_UPDATE == action || Intent.ACTION_BOOT_COMPLETED == action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidget = ComponentName(context.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

            // Update all widgets
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }

            // Reschedule the next alarm
            scheduleNextUpdate(context)
        }
    }

    private fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getUpdatePendingIntent(context)

        val now = Calendar.getInstance()

        // Define the time changeover points
        val morningStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 5); set(Calendar.MINUTE, 0) }
        val dayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }
        val nightStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0) }

        // Determine the next trigger time
        val nextTriggerMillis = when {
            now.before(morningStart) -> morningStart.timeInMillis
            now.before(dayStart) -> dayStart.timeInMillis
            now.before(nightStart) -> nightStart.timeInMillis
            else -> {
                // After 6 PM, schedule for 5 AM tomorrow
                morningStart.add(Calendar.DAY_OF_YEAR, 1)
                morningStart.timeInMillis
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("TouhouWidgetProvider", "Cannot schedule exact alarms. App may not update precisely on time.")
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerMillis, pendingIntent)
        }
    }

    private fun getUpdatePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TouhouWidgetProvider::class.java).apply {
            action = ACTION_AUTO_UPDATE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    companion object {
        private const val ACTION_AUTO_UPDATE = "com.example.touhoundw.action.AUTO_UPDATE"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            val imageResource = when (hour) {
                in 5..8 -> R.drawable.koishi_01 // 5:00 AM - 8:59 AM
                in 9..17 -> R.drawable.reimu_01   // 9:00 AM - 5:59 PM
                else -> R.drawable.doremy_01    // All other times
            }
            views.setImageViewResource(R.id.widget_image, imageResource)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
