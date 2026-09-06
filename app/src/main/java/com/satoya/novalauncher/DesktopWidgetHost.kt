package com.satoya.novalauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.ViewGroup

class DesktopWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
    private val appContext = context.applicationContext
    private val appWidgetManager = AppWidgetManager.getInstance(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun storedClockWidgetId(): Int? {
        val id = prefs.getInt(KEY_CLOCK_WIDGET_ID, INVALID_APPWIDGET_ID)
        if (id == INVALID_APPWIDGET_ID || appWidgetManager.getAppWidgetInfo(id) == null) {
            if (id != INVALID_APPWIDGET_ID) deleteAppWidgetId(id)
            prefs.edit().remove(KEY_CLOCK_WIDGET_ID).apply()
            return null
        }
        return id
    }

    fun installClockWidget(): Int? {
        storedClockWidgetId()?.let { return it }

        val appWidgetId = allocateAppWidgetId()
        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            ClockWidgetProvider.provider(appContext)
        )
        if (!bound) {
            deleteAppWidgetId(appWidgetId)
            return null
        }

        prefs.edit().putInt(KEY_CLOCK_WIDGET_ID, appWidgetId).apply()
        return appWidgetId
    }

    fun createClockWidgetView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return null
        return createView(context, appWidgetId, info).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    companion object {
        private const val HOST_ID = 0x4E4F5641
        private const val PREFS_NAME = "launcher_prefs"
        private const val KEY_CLOCK_WIDGET_ID = "clock_widget_id"
        private const val INVALID_APPWIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID
    }
}
