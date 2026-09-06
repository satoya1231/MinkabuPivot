package com.satoya.novalauncher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ClockWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_LAUNCH_FAVORITE) {
            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
            val activityName = intent.getStringExtra(EXTRA_ACTIVITY_NAME) ?: return
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(ComponentName(packageName, activityName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
        }
    }

    companion object {
        fun provider(context: Context): ComponentName =
            ComponentName(context, ClockWidgetProvider::class.java)

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(provider(context))
            if (ids.isNotEmpty()) {
                ClockWidgetProvider().onUpdate(context, manager, ids)
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_favorites_grid)
            }
        }

        private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            views.setOnClickPendingIntent(
                R.id.widget_clock_root,
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_menu_button,
                PendingIntent.getActivity(
                    context,
                    appWidgetId + 10000,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            val serviceIntent = Intent(context, FavoritesWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_favorites_grid, serviceIntent)
            views.setEmptyView(R.id.widget_favorites_grid, R.id.widget_favorites_empty)
            views.setInt(
                R.id.widget_favorites_grid,
                "setNumColumns",
                columnsFor(context, appWidgetId)
            )
            val launchTemplate = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                Intent(context, ClockWidgetProvider::class.java).setAction(ACTION_LAUNCH_FAVORITE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_favorites_grid, launchTemplate)
            return views
        }

        private fun columnsFor(context: Context, appWidgetId: Int): Int {
            val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            return (widthDp / 64).coerceIn(1, 4)
        }

        private const val ACTION_LAUNCH_FAVORITE =
            "com.satoya.novalauncher.action.LAUNCH_FAVORITE"
        private const val EXTRA_PACKAGE_NAME = "favorite_package_name"
        private const val EXTRA_ACTIVITY_NAME = "favorite_activity_name"
    }
}
